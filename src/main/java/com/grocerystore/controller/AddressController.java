package com.grocerystore.controller;

import com.grocerystore.dto.request.AddressRequest;
import com.grocerystore.dto.response.AddressResponse;
import com.grocerystore.dto.response.ApiResponse;
import com.grocerystore.repository.UserRepository;
import com.grocerystore.service.impl.AddressService;
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
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
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
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getUserAddresses(getUserId(userDetails))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.addAddress(getUserId(userDetails), request), "Address added"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.updateAddress(id, getUserId(userDetails), request), "Address updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long id) {
        addressService.deleteAddress(id, getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable @NonNull Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.setDefaultAddress(id, getUserId(userDetails)), "Default address set"));
    }
}
