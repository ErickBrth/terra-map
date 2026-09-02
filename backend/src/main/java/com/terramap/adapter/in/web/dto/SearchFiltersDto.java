package com.terramap.adapter.in.web.dto;

import com.terramap.domain.model.ParcelStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Optional search filters, applied in addition to the circular search area")
public record SearchFiltersDto(
        @Schema(example = "500000.00")
        BigDecimal maxPrice,

        @Schema(example = "AVAILABLE")
        ParcelStatus status
) {}
