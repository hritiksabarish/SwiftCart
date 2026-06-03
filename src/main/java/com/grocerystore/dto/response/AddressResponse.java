package com.grocerystore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String label;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
    private Boolean isDefault;
}
