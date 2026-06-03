package com.grocerystore.controller;

import com.grocerystore.dto.request.CategoryRequest;
import com.grocerystore.dto.request.ProductRequest;
import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.dto.response.CategoryResponse;
import com.grocerystore.dto.response.OrderResponse;
import com.grocerystore.dto.response.ProductResponse;
import com.grocerystore.dto.response.UserResponse;
import com.grocerystore.entity.Order;
import com.grocerystore.repository.OrderRepository;
import com.grocerystore.repository.ProductRepository;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.service.impl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // -------- Products --------
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.createProduct(request), "Product created"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable @NonNull Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request), "Product updated"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable @NonNull Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated"));
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable @NonNull Long id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateStock(id, body.get("stock")), "Stock updated"));
    }

    // -------- Categories --------
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.createCategory(request), "Category created"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable @NonNull Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, request), "Category updated"));
    }

    // -------- Orders --------
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Objects.requireNonNull(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable @NonNull Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrderStatus(id, body.get("status")), "Order status updated"));
    }

    @GetMapping("/orders/stats")
    public ResponseEntity<ApiResponse<Object>> getOrderStats() {
        List<Object[]> stats = orderRepository.countByOrderStatus();
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : stats) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // -------- Users --------
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Objects.requireNonNull(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable)));
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUser(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserStatus(id), "User status updated"));
    }

    // -------- Dashboard Stats --------
    @GetMapping("/products/low-stock")
    public ResponseEntity<ApiResponse<Object>> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(ApiResponse.success(productRepository.findByStockQuantityLessThanAndIsActiveTrue(threshold)));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats() {
        BigDecimal revenue = orderRepository.getTotalRevenue();
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIDNIGHT);
        long ordersToday = orderRepository.countOrdersToday(startOfDay);
        long totalUsers = userRepository.count();
        long lowStock = productRepository.findByStockQuantityLessThanAndIsActiveTrue(10).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", revenue);
        stats.put("ordersToday", ordersToday);
        stats.put("totalUsers", totalUsers);
        stats.put("lowStockAlerts", lowStock);
        stats.put("totalProducts", productRepository.countByIsActiveTrue());

        // Order status breakdown
        List<Object[]> orderStats = orderRepository.countByOrderStatus();
        Map<String, Long> orderStatusMap = new HashMap<>();
        for (Object[] row : orderStats) {
            orderStatusMap.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        stats.put("ordersByStatus", orderStatusMap);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
