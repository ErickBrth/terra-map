package com.terramap.adapter.in.web;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.application.port.in.GetLandParcelUseCase;
import com.terramap.application.port.in.RegisterLandParcelUseCase;
import com.terramap.application.port.in.SearchLandParcelsUseCase;
import com.terramap.application.port.in.UpdateParcelStatusUseCase;
import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import com.terramap.domain.service.GeometryValidationException;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LandParcelController.class)
class LandParcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterLandParcelUseCase registerLandParcelUseCase;

    @MockitoBean
    private SearchLandParcelsUseCase searchLandParcelsUseCase;

    @MockitoBean
    private GetLandParcelUseCase getLandParcelUseCase;

    @MockitoBean
    private UpdateParcelStatusUseCase updateParcelStatusUseCase;

    private static final Money PRICE = new Money(new BigDecimal("250000.00"), "BRL");
    private static final ContactInfo CONTACT = new ContactInfo("Jane Doe", "jane@example.com", "+55 11 90000-0000");

    private LandParcel sampleParcel() {
        return LandParcel.create("Riverside lot", "Flat terrain", PRICE, CONTACT, GeometryFixtures.saoPauloParcelA());
    }

    private String validRegisterRequestJson() {
        return """
                {
                  "title": "Riverside lot",
                  "description": "Flat terrain with river access.",
                  "totalPrice": 250000.00,
                  "currency": "BRL",
                  "contact": { "name": "Jane Doe", "email": "jane@example.com", "phone": "+55 11 90000-0000" },
                  "boundary": {
                    "type": "Polygon",
                    "coordinates": [[[-46.635, -23.555], [-46.625, -23.555], [-46.625, -23.545], [-46.635, -23.545], [-46.635, -23.555]]]
                  }
                }
                """;
    }

    // ── POST /api/v1/parcels ────────────────────────────────────────────────

    @Test
    void registerReturns201WithLocationHeaderOnSuccess() throws Exception {
        LandParcel created = sampleParcel();
        when(registerLandParcelUseCase.register(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Riverside lot"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void registerReturns400WhenTitleIsMissing() throws Exception {
        String invalidJson = """
                {
                  "totalPrice": 250000.00,
                  "contact": { "name": "Jane Doe", "email": "jane@example.com" },
                  "boundary": {
                    "type": "Polygon",
                    "coordinates": [[[-46.635, -23.555], [-46.625, -23.555], [-46.625, -23.545], [-46.635, -23.545], [-46.635, -23.555]]]
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void registerReturns409WhenBoundaryOverlaps() throws Exception {
        UUID conflictingId = UUID.randomUUID();
        when(registerLandParcelUseCase.register(any()))
                .thenThrow(new OverlappingParcelException(List.of(conflictingId)));

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflictingParcelIds[0]").value(conflictingId.toString()));
    }

    @Test
    void registerReturns422WhenGeometryIsInvalid() throws Exception {
        when(registerLandParcelUseCase.register(any()))
                .thenThrow(new GeometryValidationException("boundary self-intersects"));

        mockMvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterRequestJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("boundary self-intersects"));
    }

    // ── POST /api/v1/parcels/search ─────────────────────────────────────────

    @Test
    void searchReturnsFeatureCollectionOfMatchingParcels() throws Exception {
        LandParcel found = sampleParcel();
        when(searchLandParcelsUseCase.search(any())).thenReturn(List.of(found));

        String searchJson = """
                {
                  "center": { "type": "Point", "coordinates": [-46.63, -23.55] },
                  "radiusInMeters": 1500
                }
                """;

        mockMvc.perform(post("/api/v1/parcels/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features[0].properties.title").value("Riverside lot"))
                // Fluxo B shows full contact info, same as GET /{id} — no masking rule in scope
                .andExpect(jsonPath("$.features[0].properties.contact.email").value("jane@example.com"));
    }

    @Test
    void searchReturns400WhenRadiusExceedsMaximum() throws Exception {
        String searchJson = """
                {
                  "center": { "type": "Point", "coordinates": [-46.63, -23.55] },
                  "radiusInMeters": 999999
                }
                """;

        mockMvc.perform(post("/api/v1/parcels/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsEmptyFeatureCollectionWhenNothingMatches() throws Exception {
        when(searchLandParcelsUseCase.search(any())).thenReturn(List.of());

        String searchJson = """
                {
                  "center": { "type": "Point", "coordinates": [-46.63, -23.55] },
                  "radiusInMeters": 500
                }
                """;

        mockMvc.perform(post("/api/v1/parcels/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features").isEmpty());
    }

    // ── GET /api/v1/parcels/{id} ─────────────────────────────────────────────

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        LandParcel parcel = sampleParcel();
        when(getLandParcelUseCase.getById(parcel.getId())).thenReturn(parcel);

        mockMvc.perform(get("/api/v1/parcels/{id}", parcel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parcel.getId().toString()))
                // full endpoint must NOT mask the email
                .andExpect(jsonPath("$.contact.email").value("jane@example.com"));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(getLandParcelUseCase.getById(missingId)).thenThrow(new ParcelNotFoundException(missingId));

        mockMvc.perform(get("/api/v1/parcels/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdOnMalformedUuidReturns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/parcels/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    // ── PATCH /api/v1/parcels/{id}/reserve ──────────────────────────────────

    @Test
    void reserveReturns200WithUpdatedStatus() throws Exception {
        LandParcel parcel = sampleParcel();
        parcel.markReserved();
        when(updateParcelStatusUseCase.reserve(parcel.getId())).thenReturn(parcel);

        mockMvc.perform(patch("/api/v1/parcels/{id}/reserve", parcel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void reserveReturns404WhenParcelNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(updateParcelStatusUseCase.reserve(missingId)).thenThrow(new ParcelNotFoundException(missingId));

        mockMvc.perform(patch("/api/v1/parcels/{id}/reserve", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserveReturns409WhenParcelIsNotAvailable() throws Exception {
        UUID soldId = UUID.randomUUID();
        when(updateParcelStatusUseCase.reserve(soldId))
                .thenThrow(new IllegalStateException("Only AVAILABLE parcels can be reserved"));

        mockMvc.perform(patch("/api/v1/parcels/{id}/reserve", soldId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Only AVAILABLE parcels can be reserved"));
    }

    // ── PATCH /api/v1/parcels/{id}/sell ─────────────────────────────────────

    @Test
    void markSoldReturns200WithUpdatedStatus() throws Exception {
        LandParcel parcel = sampleParcel();
        parcel.markSold();
        when(updateParcelStatusUseCase.markSold(parcel.getId())).thenReturn(parcel);

        mockMvc.perform(patch("/api/v1/parcels/{id}/sell", parcel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    void markSoldReturns404WhenParcelNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(updateParcelStatusUseCase.markSold(missingId)).thenThrow(new ParcelNotFoundException(missingId));

        mockMvc.perform(patch("/api/v1/parcels/{id}/sell", missingId))
                .andExpect(status().isNotFound());
    }
}