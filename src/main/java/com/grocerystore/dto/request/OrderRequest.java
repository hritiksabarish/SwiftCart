package com.grocerystore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {
    @NotNull(message = "Address ID is required")
    private Long addressId;

    private String paymentMethod = "CASH_ON_DELIVERY";
    private String deliveryInstructions;
}
