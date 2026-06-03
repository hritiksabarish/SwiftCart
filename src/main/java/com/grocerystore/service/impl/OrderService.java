package com.grocerystore.service.impl;

import com.grocerystore.dto.request.OrderRequest;
import com.grocerystore.dto.response.AddressResponse;
import com.grocerystore.dto.response.OrderResponse;
import com.grocerystore.entity.*;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public OrderResponse placeOrder(@NonNull Long userId, OrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty())
            throw new BadRequestException("Cart is empty");

        Long addressId = Objects.requireNonNull(request.getAddressId(),
                "addressId must not be null");

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId))
            throw new BadRequestException("Address does not belong to current user");

        // Validate stock and build order items
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity())
                throw new BadRequestException("Insufficient stock for: " + product.getName());

            BigDecimal effectivePrice = product.getDiscountPrice() != null ?
                    product.getDiscountPrice() : product.getPrice();
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(effectivePrice)
                    .subtotal(subtotal)
                    .build();
            orderItems.add(orderItem);
            total = total.add(subtotal);

            // Reduce stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(Objects.requireNonNull(product));
        }

        Order order = Order.builder()
                .user(User.builder().id(userId).build())
                .address(address)
                .totalAmount(total)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(total)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH_ON_DELIVERY")
                .orderStatus(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .deliveryInstructions(request.getDeliveryInstructions())
                .build();

        Order savedOrder = orderRepository.save(Objects.requireNonNull(order));
        orderItems.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(Objects.requireNonNull(savedOrder));

        // Create payment record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .method(savedOrder.getPaymentMethod())
                .amount(savedOrder.getFinalAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();
        paymentRepository.save(Objects.requireNonNull(payment));

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(Objects.requireNonNull(cart));

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(@NonNull Long userId, @NonNull Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(@NonNull Long orderId, @NonNull Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check ownership (unless admin view — handled at controller level)
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(@NonNull Long orderId, @NonNull Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId))
            throw new BadRequestException("Order does not belong to current user");

        if (order.getOrderStatus() != Order.OrderStatus.PENDING)
            throw new BadRequestException("Only PENDING orders can be cancelled");

        // Restore stock
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(Objects.requireNonNull(product));
            }
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(Objects.requireNonNull(order)));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(@NonNull Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(@NonNull Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            order.setOrderStatus(Order.OrderStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }

        return toResponse(orderRepository.save(Objects.requireNonNull(order)));
    }

    private OrderResponse toResponse(Order order) {
        Address addr = order.getAddress();
        AddressResponse addressResponse = addr != null ? AddressResponse.builder()
                .id(addr.getId())
                .label(addr.getLabel())
                .street(addr.getStreet())
                .city(addr.getCity())
                .state(addr.getState())
                .pincode(addr.getPincode())
                .landmark(addr.getLandmark())
                .isDefault(addr.getIsDefault())
                .build() : null;

        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems() == null ? List.of() :
                order.getOrderItems().stream().map(item -> {
                    Product p = item.getProduct();
                    return OrderResponse.OrderItemResponse.builder()
                            .id(item.getId())
                            .productId(p.getId())
                            .productName(p.getName())
                            .productImage(p.getImageUrl())
                            .brand(p.getBrand())
                            .unit(p.getUnit())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build();
                }).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .customerName(order.getUser().getName())
                .address(addressResponse)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .orderStatus(order.getOrderStatus().name())
                .deliveryInstructions(order.getDeliveryInstructions())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(itemResponses)
                .build();
    }
}
