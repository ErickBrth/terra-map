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
    /** {@code returned} is the size of this page, not a count across all pages —
     * computing a true total would require a second COUNT query the MVP scope
     * doesn't need yet. Named accordingly so the field can't be misread as one. */
    public record Metadata(
            int returned,
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
