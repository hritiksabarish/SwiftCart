package com.grocerystore.service.impl;

import com.grocerystore.dto.request.AddressRequest;
import com.grocerystore.dto.response.AddressResponse;
import com.grocerystore.entity.Address;
import com.grocerystore.entity.User;
import com.grocerystore.exception.BadRequestException;
import com.grocerystore.exception.ResourceNotFoundException;
import com.grocerystore.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> getUserAddresses(@NonNull Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse addAddress(@NonNull Long userId, AddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultAddress(userId);
        }

        Address address = Address.builder()
                .user(User.builder().id(userId).build())
                .label(request.getLabel())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        return toResponse(addressRepository.save(Objects.requireNonNull(address)));
    }

    @Transactional
    public AddressResponse updateAddress(@NonNull Long addressId, @NonNull Long userId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId))
            throw new BadRequestException("Address does not belong to current user");

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultAddress(userId);
        }

        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        return toResponse(addressRepository.save(Objects.requireNonNull(address)));
    }

    @Transactional
    public void deleteAddress(@NonNull Long addressId, @NonNull Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId))
            throw new BadRequestException("Address does not belong to current user");

        addressRepository.delete(Objects.requireNonNull(address));
    }

    @Transactional
    public AddressResponse setDefaultAddress(@NonNull Long addressId, @NonNull Long userId) {
        clearDefaultAddress(userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId))
            throw new BadRequestException("Address does not belong to current user");

        address.setIsDefault(true);
        return toResponse(addressRepository.save(Objects.requireNonNull(address)));
    }

    private void clearDefaultAddress(@NonNull Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setIsDefault(false);
            addressRepository.save(Objects.requireNonNull(addr));
        });
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .landmark(address.getLandmark())
                .isDefault(address.getIsDefault())
                .build();
    }
}
