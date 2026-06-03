package com.grocerystore.service.impl;

import com.grocerystore.dto.request.CartItemRequest;
import com.grocerystore.dto.response.CartResponse;
import com.grocerystore.entity.Cart;
import com.grocerystore.entity.CartItem;
import com.grocerystore.entity.Product;
import com.grocerystore.entity.User;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.CartItemRepository;
import com.grocerystore.repository.CartRepository;
import com.grocerystore.repository.ProductRepository;
import com.grocerystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(@NonNull Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(@NonNull Long userId, CartItemRequest request) {
        Long productId = Objects.requireNonNull(request.getProductId(),
                "productId must not be null");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getIsActive()) throw new BadRequestException("Product is not available");
        if (product.getStockQuantity() < request.getQuantity())
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());

        Cart cart = getOrCreateCart(userId);

        Long cartId    = Objects.requireNonNull(cart.getId(), "cart id must not be null");
        Long prodId    = Objects.requireNonNull(product.getId(), "product id must not be null");

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductId(cartId, prodId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQty)
                throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
            item.setQuantity(newQty);
            cartItemRepository.save(Objects.requireNonNull(item));
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(Objects.requireNonNull(item));
        }

        return toResponse(cartRepository.findById(cartId).orElseThrow());
    }

    @Transactional
    public CartResponse updateItem(@NonNull Long userId, CartItemRequest request) {
        Long productId = Objects.requireNonNull(request.getProductId(),
                "productId must not be null");

        Cart cart = getOrCreateCart(userId);
        Long cartId = Objects.requireNonNull(cart.getId(), "cart id must not be null");

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart"));

        Product product = item.getProduct();
        if (product.getStockQuantity() < request.getQuantity())
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(Objects.requireNonNull(item));
        return toResponse(cartRepository.findById(cartId).orElseThrow());
    }

    @Transactional
    public CartResponse removeItem(@NonNull Long userId, @NonNull Long itemId) {
        Cart cart = getOrCreateCart(userId);
        Long cartId = Objects.requireNonNull(cart.getId(), "cart id must not be null");

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cartId))
            throw new BadRequestException("Item does not belong to your cart");

        cartItemRepository.delete(Objects.requireNonNull(item));
        return toResponse(cartRepository.findById(cartId).orElseThrow());
    }

    @Transactional
    public void clearCart(@NonNull Long userId) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cart.getItems().clear();
            cartRepository.save(Objects.requireNonNull(cart));
        }
    }

    private Cart getOrCreateCart(@NonNull Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = Cart.builder().user(user).items(new ArrayList<>()).build();
            return cartRepository.save(Objects.requireNonNull(cart));
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cart.getItems() == null ? List.of() :
                cart.getItems().stream().map(item -> {
                    Product p = item.getProduct();
                    BigDecimal effectivePrice = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getPrice();
                    return CartResponse.CartItemResponse.builder()
                            .id(item.getId())
                            .productId(p.getId())
                            .productName(p.getName())
                            .productImage(p.getImageUrl())
                            .brand(p.getBrand())
                            .unit(p.getUnit())
                            .price(p.getPrice())
                            .discountPrice(p.getDiscountPrice())
                            .quantity(item.getQuantity())
                            .itemTotal(effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                            .availableStock(p.getStockQuantity())
                            .build();
                }).collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(CartResponse.CartItemResponse::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .subtotal(subtotal)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }
}
