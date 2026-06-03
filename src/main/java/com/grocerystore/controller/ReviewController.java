package com.grocerystore.controller;

import com.grocerystore.dto.request.ReviewRequest;
import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.dto.response.ReviewResponse;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.service.impl.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
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

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProductReviews(@PathVariable @NonNull Long productId) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductReviews(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.addReview(getUserId(userDetails), request), "Review added"));
    }
}
