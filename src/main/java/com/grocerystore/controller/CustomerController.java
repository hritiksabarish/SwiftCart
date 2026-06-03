package com.grocerystore.controller;

import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.repository.CartRepository;
import com.grocerystore.repository.OrderRepository;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @GetMapping("/dashboard/stats")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);

        long totalOrders = orderRepository.countByUserIdAndOrderStatusNot(userId,
                com.grocerystore.entity.Order.OrderStatus.CANCELLED);
        long pendingOrders = orderRepository.countByUserIdAndOrderStatus(userId,
                com.grocerystore.entity.Order.OrderStatus.PENDING);
        BigDecimal totalSpent = orderRepository.getTotalSpentByUser(userId);
        long wishlistItems = wishlistRepository.countByUserId(userId);

        int cartItems = cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems() != null ?
                        cart.getItems().stream().mapToInt(i -> i.getQuantity()).sum() : 0)
                .orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("totalSpent", totalSpent != null ? totalSpent : BigDecimal.ZERO);
        stats.put("wishlistItems", wishlistItems);
        stats.put("cartItems", cartItems);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
