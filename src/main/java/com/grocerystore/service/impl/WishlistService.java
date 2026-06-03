package com.grocerystore.service.impl;

import com.grocerystore.dto.response.WishlistResponse;
import com.grocerystore.entity.Product;
import com.grocerystore.entity.User;
import com.grocerystore.entity.Wishlist;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.ProductRepository;
import com.grocerystore.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(@NonNull Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistResponse addToWishlist(@NonNull Long userId, @NonNull Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId))
            throw new BadRequestException("Product already in wishlist");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Wishlist wishlist = Wishlist.builder()
                .user(User.builder().id(userId).build())
                .product(product)
                .build();

        // Objects.requireNonNull() returns @NonNull T, satisfying save(@NonNull S)
        return toResponse(wishlistRepository.save(Objects.requireNonNull(wishlist)));
    }

    @Transactional
    public void removeFromWishlist(@NonNull Long userId, @NonNull Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId))
            throw new ResourceNotFoundException("Product not in wishlist");

        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .product(productService.toResponse(wishlist.getProduct()))
                .addedAt(wishlist.getAddedAt())
                .build();
    }
}
