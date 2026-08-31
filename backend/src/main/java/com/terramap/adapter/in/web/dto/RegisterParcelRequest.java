package com.terramap.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Payload for registering a new land parcel listing")
public record RegisterParcelRequest(
        @Schema(example = "Riverside lot")
        @NotBlank @Size(max = 120) String title,

        @Schema(example = "Flat terrain with river access.")
        @Size(max = 2000) String description,

        @Schema(example = "250000.00")
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalPrice,

        @Schema(example = "BRL", defaultValue = "BRL")
        String currency,

        @NotNull @Valid ContactDto contact,

        @NotNull @Valid GeoJsonPolygonDto boundary
) {}
