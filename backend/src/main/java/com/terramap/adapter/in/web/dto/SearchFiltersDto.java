package com.terramap.adapter.in.web.dto;

import com.terramap.domain.model.ParcelStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Optional search filters")
public record SearchFiltersDto(
        @Schema(example = "500000.00")
        BigDecimal maxPrice,

        @Schema(example = "300.0")
        Double minAreaInSquareMeters,

        @Schema(example = "AVAILABLE")
        ParcelStatus status
) {}
