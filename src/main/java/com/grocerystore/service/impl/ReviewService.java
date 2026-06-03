package com.grocerystore.service.impl;

import com.grocerystore.dto.request.ReviewRequest;
import com.grocerystore.dto.response.ReviewResponse;
import com.grocerystore.entity.Product;
import com.grocerystore.entity.Review;
import com.grocerystore.entity.User;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.ProductRepository;
import com.grocerystore.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public List<ReviewResponse> getProductReviews(@NonNull Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ReviewResponse addReview(@NonNull Long userId, ReviewRequest request) {
        // request.getProductId() may be unvalidated — guard it before passing to @NonNull methods
        Long productId = Objects.requireNonNull(request.getProductId(),
                "productId must not be null");

        if (reviewRepository.existsByProductIdAndUserId(productId, userId))
            throw new BadRequestException("You have already reviewed this product");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Review review = Review.builder()
                .product(product)
                .user(User.builder().id(userId).build())
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .isVerifiedPurchase(false)
                .build();

        return toResponse(reviewRepository.save(Objects.requireNonNull(review)));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
