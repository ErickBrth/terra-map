package com.terramap.adapter.in.web.dto;

import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.ParcelStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "GeoJSON Feature representing a land parcel")
public record ParcelFeatureDto(
        @Schema(example = "Feature")
        String type,

        UUID id,

        GeoJsonPolygonDto geometry,

        Properties properties
) {
    public record Properties(
            String title,
            String description,
            BigDecimal totalPrice,
            String currency,
            ParcelStatus status,
            ContactDto contact,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public static ParcelFeatureDto fromDomain(LandParcel parcel) {
        return new ParcelFeatureDto(
                "Feature",
                parcel.getId(),
                GeoJsonPolygonDto.fromJtsPolygon(parcel.getBoundary()),
                new Properties(
                        parcel.getTitle(),
                        parcel.getDescription(),
                        parcel.getTotalPrice().getAmount(),
                        parcel.getTotalPrice().getCurrency(),
                        parcel.getStatus(),
                        new ContactDto(
                                parcel.getContact().getName(),
                                parcel.getContact().getEmail(),
                                parcel.getContact().getPhone()
                        ),
                        parcel.getCreatedAt(),
                        parcel.getUpdatedAt()
                )
        );
    }
}
