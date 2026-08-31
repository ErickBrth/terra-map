package com.terramap.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "GeoJSON FeatureCollection representing parcel search results")
public record ParcelFeatureCollectionDto(
        @Schema(example = "FeatureCollection")
        String type,

        List<ParcelFeatureDto> features,

        Metadata metadata
) {
    public record Metadata(
            int total,
            int page,
            int size,
            double radiusInMeters
    ) {}

    public static ParcelFeatureCollectionDto of(List<ParcelFeatureDto> features, int page, int size, double radiusInMeters) {
        return new ParcelFeatureCollectionDto(
                "FeatureCollection",
                features,
                new Metadata(features.size(), page, size, radiusInMeters)
        );
    }
}
