package com.grocerystore.controller;

import com.grocerystore.dto.request.OrderRequest;
import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.dto.response.OrderResponse;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.service.impl.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @NonNull
    private Long getUserId(UserDetails userDetails) {
        return Objects.requireNonNull(
                userRepository.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"))
                        .getId(),
                "user id must not be null"
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.placeOrder(getUserId(userDetails), request), "Order placed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = Objects.requireNonNull(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getUserOrders(getUserId(userDetails), pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(id, getUserId(userDetails))));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(id, getUserId(userDetails)), "Order cancelled"));
    }
}
