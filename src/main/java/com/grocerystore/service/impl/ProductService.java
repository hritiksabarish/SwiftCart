package com.grocerystore.service.impl;

import com.grocerystore.dto.request.ProductRequest;
import com.grocerystore.dto.response.ProductResponse;
import com.grocerystore.entity.Category;
import com.grocerystore.entity.Product;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.CategoryRepository;
import com.grocerystore.repository.ProductRepository;
import com.grocerystore.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(@NonNull Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(@NonNull Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, @NonNull Pageable pageable) {
        return productRepository.searchProducts(query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(@NonNull Long categoryId, @NonNull Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedProducts(@NonNull Pageable pageable) {
        return productRepository.findFeaturedProducts(pageable).map(this::toResponse);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Long categoryId = Objects.requireNonNull(request.getCategoryId(),
                "categoryId must not be null");

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(category)
                .brand(request.getBrand())
                .unit(request.getUnit())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(productRepository.save(Objects.requireNonNull(product)));
    }

    public ProductResponse updateProduct(@NonNull Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Long categoryId = Objects.requireNonNull(request.getCategoryId(),
                "categoryId must not be null");

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        return toResponse(productRepository.save(Objects.requireNonNull(product)));
    }

    public void deleteProduct(@NonNull Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(Objects.requireNonNull(product));
    }

    public ProductResponse updateStock(@NonNull Long id, Integer stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setStockQuantity(stock);
        return toResponse(productRepository.save(Objects.requireNonNull(product)));
    }

    public ProductResponse toResponse(Product product) {
        Long productId = Objects.requireNonNull(product.getId(),
                "product id must not be null");

        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        int reviewCount = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).size();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brand(product.getBrand())
                .unit(product.getUnit())
                .isActive(product.getIsActive())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
