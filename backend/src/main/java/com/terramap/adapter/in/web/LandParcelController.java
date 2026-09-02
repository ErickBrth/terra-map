package com.terramap.adapter.in.web;

import com.terramap.adapter.in.web.dto.*;
import com.terramap.application.port.in.GetLandParcelUseCase;
import com.terramap.application.port.in.RegisterLandParcelUseCase;
import com.terramap.application.port.in.SearchLandParcelsUseCase;
import com.terramap.application.port.in.UpdateParcelStatusUseCase;
import com.terramap.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parcels")
@Tag(name = "Land Parcels", description = "Endpoints for land parcel registration, spatial search, and retrieval")
public class LandParcelController {

    private final RegisterLandParcelUseCase registerLandParcelUseCase;
    private final SearchLandParcelsUseCase searchLandParcelsUseCase;
    private final GetLandParcelUseCase getLandParcelUseCase;
    private final UpdateParcelStatusUseCase updateParcelStatusUseCase;

    public LandParcelController(RegisterLandParcelUseCase registerLandParcelUseCase,
                                SearchLandParcelsUseCase searchLandParcelsUseCase,
                                GetLandParcelUseCase getLandParcelUseCase,
                                UpdateParcelStatusUseCase updateParcelStatusUseCase) {
        this.registerLandParcelUseCase = registerLandParcelUseCase;
        this.searchLandParcelsUseCase = searchLandParcelsUseCase;
        this.getLandParcelUseCase = getLandParcelUseCase;
        this.updateParcelStatusUseCase = updateParcelStatusUseCase;
    }

    @PostMapping
    @Operation(summary = "Register a new land parcel", description = "Creates a new parcel listing if its boundary does not overlap with existing parcels")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Parcel successfully registered",
                    content = @Content(schema = @Schema(implementation = LandParcelResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation error"),
            @ApiResponse(responseCode = "409", description = "Parcel boundary overlaps an existing registered parcel"),
            @ApiResponse(responseCode = "422", description = "Invalid geometry (self-intersection, unclosed ring, etc.)")
    })
    public ResponseEntity<LandParcelResponse> register(@Valid @RequestBody RegisterParcelRequest request) {
        Polygon boundary = request.boundary().toJtsPolygon();
        String currency = request.currency() != null && !request.currency().isBlank() ? request.currency() : "BRL";
        Money money = new Money(request.totalPrice(), currency);
        ContactInfo contact = new ContactInfo(
                request.contact().name(),
                request.contact().email(),
                request.contact().phone()
        );

        RegisterLandParcelUseCase.Command command = new RegisterLandParcelUseCase.Command(
                request.title(),
                request.description(),
                money,
                contact,
                boundary
        );

        LandParcel created = registerLandParcelUseCase.register(command);
        LandParcelResponse response = LandParcelResponse.fromDomain(created);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/search")
    @Operation(summary = "Search parcels within a circular radius", description = "Returns GeoJSON FeatureCollection of parcels intersecting the search circle, optionally filtered by max price and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                    content = @Content(schema = @Schema(implementation = ParcelFeatureCollectionDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    public ResponseEntity<ParcelFeatureCollectionDto> search(@Valid @RequestBody SearchParcelRequest request) {
        Point center = request.center().toJtsPoint();
        SearchArea searchArea = new SearchArea(center, request.radiusInMeters());
        SearchFiltersDto filters = request.filters();

        SearchLandParcelsUseCase.Query query = new SearchLandParcelsUseCase.Query(
                searchArea,
                filters != null ? filters.maxPrice() : null,
                filters != null ? filters.status() : null,
                request.effectivePage(),
                request.effectiveSize()
        );

        List<LandParcel> results = searchLandParcelsUseCase.search(query);
        List<ParcelFeatureDto> features = results.stream()
                .map(ParcelFeatureDto::fromDomain)
                .toList();

        ParcelFeatureCollectionDto collection = ParcelFeatureCollectionDto.of(
                features,
                request.effectivePage(),
                request.effectiveSize(),
                request.radiusInMeters()
        );

        return ResponseEntity.ok(collection);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get land parcel details", description = "Returns complete parcel details including unmasked contact info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parcel found",
                    content = @Content(schema = @Schema(implementation = LandParcelResponse.class))),
            @ApiResponse(responseCode = "404", description = "Parcel not found")
    })
    public ResponseEntity<LandParcelResponse> getById(@PathVariable("id") UUID id) {
        LandParcel parcel = getLandParcelUseCase.getById(id);
        return ResponseEntity.ok(LandParcelResponse.fromDomain(parcel));
    }

    @PatchMapping("/{id}/reserve")
    @Operation(summary = "Reserve a parcel", description = "Marks an AVAILABLE parcel as RESERVED, taking it off the market while a deal is negotiated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parcel reserved",
                    content = @Content(schema = @Schema(implementation = LandParcelResponse.class))),
            @ApiResponse(responseCode = "404", description = "Parcel not found"),
            @ApiResponse(responseCode = "409", description = "Parcel is not currently AVAILABLE")
    })
    public ResponseEntity<LandParcelResponse> reserve(@PathVariable("id") UUID id) {
        LandParcel parcel = updateParcelStatusUseCase.reserve(id);
        return ResponseEntity.ok(LandParcelResponse.fromDomain(parcel));
    }

    @PatchMapping("/{id}/sell")
    @Operation(summary = "Mark a parcel as sold", description = "Marks a parcel as SOLD, removing it from active search results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parcel marked as sold",
                    content = @Content(schema = @Schema(implementation = LandParcelResponse.class))),
            @ApiResponse(responseCode = "404", description = "Parcel not found")
    })
    public ResponseEntity<LandParcelResponse> markSold(@PathVariable("id") UUID id) {
        LandParcel parcel = updateParcelStatusUseCase.markSold(id);
        return ResponseEntity.ok(LandParcelResponse.fromDomain(parcel));
    }
}
