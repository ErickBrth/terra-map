package com.terramap.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for searching land parcels within a circular geographic area")
public record SearchParcelRequest(
        @NotNull @Valid GeoJsonPointDto center,

        @Schema(example = "1500.0", description = "Search radius in real-world metres (max 50,000)")
        @NotNull
        @DecimalMin(value = "1.0", message = "Radius must be at least 1 metre")
        @DecimalMax(value = "50000.0", message = "Radius cannot exceed 50,000 metres")
        Double radiusInMeters,

        @Valid SearchFiltersDto filters,

        @Schema(example = "0", defaultValue = "0")
        @Min(0) Integer page,

        @Schema(example = "100", defaultValue = "100")
        @Min(1) @Max(200) Integer size
) {
    public int effectivePage() {
        return page != null ? page : 0;
    }

    public int effectiveSize() {
        return size != null ? size : 100;
    }
}
