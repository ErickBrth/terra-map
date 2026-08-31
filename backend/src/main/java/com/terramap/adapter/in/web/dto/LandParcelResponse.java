package com.terramap.adapter.in.web.dto;

import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.ParcelStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Detailed representation of a land parcel")
public record LandParcelResponse(
        UUID id,
        String title,
        String description,
        BigDecimal totalPrice,
        String currency,
        ContactDto contact,
        ParcelStatus status,
        GeoJsonPolygonDto boundary,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public static LandParcelResponse fromDomain(LandParcel parcel) {
        return new LandParcelResponse(
                parcel.getId(),
                parcel.getTitle(),
                parcel.getDescription(),
                parcel.getTotalPrice().getAmount(),
                parcel.getTotalPrice().getCurrency(),
                new ContactDto(
                        parcel.getContact().getName(),
                        parcel.getContact().getEmail(),
                        parcel.getContact().getPhone()
                ),
                parcel.getStatus(),
                GeoJsonPolygonDto.fromJtsPolygon(parcel.getBoundary()),
                parcel.getCreatedAt(),
                parcel.getUpdatedAt(),
                parcel.getVersion()
        );
    }
}
