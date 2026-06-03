package com.grocerystore.controller;

import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.dto.response.WishlistResponse;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.service.impl.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(getUserId(userDetails))));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.addToWishlist(getUserId(userDetails), productId), "Added to wishlist"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> removeFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long productId) {
        wishlistService.removeFromWishlist(getUserId(userDetails), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }
}
