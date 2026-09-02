# TerraMap — Interactive Land Marketplace

A map-first marketplace where land parcels are advertised and discovered by drawing directly on
an interactive map. Sellers draw the exact boundary of their plot; buyers draw a circular search
area with the mouse and instantly see every parcel intersecting it.

![Backend coverage](https://img.shields.io/badge/backend%20coverage-88%25-brightgreen)
![Frontend coverage](https://img.shields.io/badge/frontend%20coverage-92%25-brightgreen)

## Table of Contents

1. [What the system is](#what-the-system-is)
2. [How it works](#how-it-works)
3. [Architecture](#architecture)
4. [Spatial design decisions](#spatial-design-decisions)
5. [Running with Docker](#running-with-docker)
6. [Running without Docker](#running-without-docker)
7. [Running the tests](#running-the-tests)
8. [API reference](#api-reference)
9. [Security considerations](#security-considerations)
10. [Scalability notes](#scalability-notes)
11. [Project structure](#project-structure)
12. [Trade-offs and what I would do next](#trade-offs-and-what-i-would-do-next)

## What the system is

TerraMap is the MVP of a land parcel marketplace where **the map is the interface** — there is no
table-based listing as the primary flow. Three actions drive the product:

- **Register a parcel** — draw the exact boundary of a plot of land on the map (snapping onto
  neighbouring parcels' edges for a clean fit), fill in a price, description, and contact, and
  publish it. The system rejects the boundary if it overlaps an already-registered parcel.
- **Search parcels** — draw a circular search area with the mouse (click and drag, the radius
  shown live as you drag, the same interaction as geojson.io), optionally filtered by maximum
  price and status, and instantly see only the parcels that intersect that circle.
- **Negotiate** — from a parcel's popup, mark it as **Reserved** while a deal is being worked out,
  or **Sold** once it's final. The map's colouring updates immediately.

Out of scope for this MVP: user authentication and messaging between buyer and seller (see
[Trade-offs](#trade-offs-and-what-i-would-do-next) for what a next iteration would add).

## How it works

**Register flow:** the user draws a polygon on the map (OpenLayers `Draw` interaction, with `Snap`
onto existing parcels so adjacent boundaries line up exactly) → the frontend converts the sketch
from the map's projection (EPSG:3857) to GeoJSON in EPSG:4326 → sends it with the parcel details to
`POST /api/v1/parcels` → the backend validates the geometry, checks for overlaps against every
existing parcel using a native PostGIS query, and either persists the new parcel or returns a
`409 Conflict` naming the parcels it overlaps with.

**Search flow:** the user draws a circle → the frontend computes the true radius in meters
(correcting for Web Mercator distortion, see below) → sends center, radius, and optional filters
(max price, status) to `POST /api/v1/parcels/search` → the backend runs a `ST_DWithin` query on a
`geography` cast, backed by a spatial index, with the filters applied as additional SQL
conditions → returns a GeoJSON `FeatureCollection` containing only the matching parcels → the
frontend replaces whatever was on the map with exactly those results.

**View & negotiate flow:** clicking a rendered parcel reads its properties (already attached to the
OpenLayers feature from either flow above) and shows them in a popup anchored to the click point,
with **Reserve** / **Mark as sold** buttons that call `PATCH /api/v1/parcels/{id}/reserve` or
`/sell`. The map updates the parcel's colour immediately on success, without a fresh search.

## Architecture

**Backend — Hexagonal (ports and adapters).** The domain (`LandParcel`, `Money`, `ContactInfo`,
`SearchArea`) has zero dependency on Spring, JPA, or HTTP — it is plain Java plus JTS geometry
types, enforced automatically by an ArchUnit test. Use cases sit in an `application` layer that
depends only on the domain and on *port* interfaces; adapters (`adapter.in.web` for REST,
`adapter.out.persistence` for JPA/PostGIS) implement those ports and are the only classes that know
about Spring or SQL.

```
   REST Controller ──implements──▶ Input Port (use case)
                                        │
                          ┌─────────────▼─────────────┐
                          │          DOMAIN            │
                          │ LandParcel, Money,         │
                          │ ContactInfo, SearchArea,   │
                          │ GeometryValidator          │
                          └─────────────┬─────────────┘
                                        │
   Output Port ◀──implements── JPA/PostGIS Adapter
```

The dependency rule (arrows always point toward the domain) is enforced by
`HexagonalArchitectureTest`, not just documented — the build fails if `domain` ever imports
`org.springframework` or `adapter`.

Status transitions (`markReserved()`, `markSold()`) live on `LandParcel` itself, not in a service —
`UpdateParcelStatusService` only loads the aggregate, delegates to it, and saves. The business rule
of which transitions are legal (e.g. only an `AVAILABLE` parcel can be reserved) is therefore
covered by fast domain unit tests, independent of the web or persistence layers.

**Frontend — feature-sliced React.** OpenLayers (imperative) and React (declarative) are kept
apart: the `ol.Map` instance lives in a single `useRef`, created once and disposed on unmount, and
every interaction with it (drawing, popups) is wrapped in a custom hook. Components never touch
`ol` objects directly, and the API layer is a single module (`api/`) — no component calls `fetch`.
A single shared factory (`toParcelFeature`) builds map features consistently whether a parcel came
from registration or from a search result.

### Design patterns

| Pattern | Where | Why |
|---|---|---|
| Ports & Adapters | Overall backend structure | Isolates domain rules from PostGIS and HTTP |
| Repository | `LandParcelRepositoryPort` | Domain speaks in `LandParcel`, never in table rows |
| DTO + explicit mapping | `adapter.in.web.dto` | Keeps the domain model from leaking into the HTTP contract (avoids mass assignment) |
| Value Object | `Money`, `ContactInfo`, `SearchArea` | Self-validating in the constructor — an invalid instance cannot exist in memory |
| Custom hook as Facade | `useOpenLayersMap`, `useDrawPolygon`, `useDrawCircle` | Hides OpenLayers' imperative API behind a small, declarative surface |
| Chain of Responsibility | `@RestControllerAdvice` | Uniform, centralized error handling across every endpoint |

## Spatial design decisions

### Why `ST_Relate(a, b, 'T********')` instead of `ST_Intersects`

| Scenario | `ST_Intersects` | `ST_Overlaps` | `ST_Relate 'T********'` |
|---|---|---|---|
| Neighbouring parcels sharing a border | blocks (**wrong**) | allows | allows |
| Partial overlap | blocks | blocks | blocks |
| One parcel fully inside another | blocks | allows (**wrong**) | blocks |
| Identical geometries | blocks | allows (**wrong**) | blocks |

Neighbouring parcels legitimately share a border in any real subdivision — a plain intersection
test would make the system unusable. `ST_Overlaps` alone doesn't work either: it returns `false`
when one polygon is entirely contained in another, which is exactly the most likely fraud case
(advertising a parcel inside one that's already listed). `ST_Relate` with the DE-9IM pattern
`T********` asserts *interior-to-interior* intersection — precisely the rule "the areas may not
overlap, but touching a border is fine."

This rule is enforced twice: once in the application layer (`findOverlappingIds`, used to return a
clear `409` with the conflicting parcel IDs), and once as a `BEFORE INSERT` trigger in the database
(migration `V3`). The application-level check is UX; the trigger is the actual guarantee — it is
the only thing that protects against two concurrent registrations racing past the application
check at the same time.

### Coordinate systems

| Where | Projection | Why |
|---|---|---|
| Database column | `geometry(Polygon, 4326)` | WGS84, degrees — what GeoJSON requires (RFC 7946) |
| API wire format | GeoJSON, EPSG:4326, `[longitude, latitude]` | Interoperable, consumed natively by OpenLayers |
| Map view | EPSG:3857 (Web Mercator) | The projection OpenLayers/OSM tiles use |
| Distance/radius measurement | PostGIS `geography` cast | Real meters on the spheroid, not degrees |

The 3857 ↔ 4326 conversion happens in exactly one module on the frontend
(`shared/geo/projection.ts`) — it is never scattered across components.

A subtler bug this project specifically guards against: OpenLayers' `Circle#getRadius()` returns
Web Mercator *projected* units, inflated by `1 / cos(latitude)`. At São Paulo's latitude (-23.5°)
that's roughly a 9% error — a circle that *looks* like 1000 m on screen is actually ~918 m on the
ground. `shared/geo/measure.ts` corrects this with `getPointResolution` before the radius is ever
sent to the API, so what the user sees drawn on the map matches what the backend actually searches.

### Indexing

Two spatial indexes back every query in this project:

- A **GiST index on `boundary`** — lets the `&&` bounding-box operator cheaply discard the vast
  majority of rows before the exact (and much more expensive) `ST_Relate` check runs on what's left.
- A **functional GiST index on `boundary::geography`** — without it, `ST_DWithin` on the geography
  cast would force a sequential scan; with it, a search over a circle stays index-backed even as
  the parcel count grows into the hundreds of thousands. The optional price/status filters are
  plain equality/range conditions evaluated after the spatial filter narrows the row set, so they
  add negligible cost.

## Running with Docker

Requirements: Docker 24+ and Docker Compose v2. Nothing else.

```bash
git clone https://github.com/ErickBrth/terra-map.git
cd terra-map
docker compose up --build
```

| Service | URL |
|---|---|
| Web app | http://localhost:8081 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |

First start takes a few minutes (Maven and npm dependency download inside the build stages).
Stop with `docker compose down`; add `-v` to also drop the database volume.

## Running without Docker

Requirements: JDK 21, Node 22, and a local PostgreSQL 16 instance with the PostGIS 3.4 extension
available (or run only the `db` service from Docker Compose and point at it).

**Backend**

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/terramap
export DB_USER=terramap
export DB_PASSWORD=terramap
./mvnw spring-boot:run
```

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

The dev server proxies `/api` to `http://localhost:8080` (configured in `vite.config.ts`), so no
CORS setup is needed locally.

## Running the tests

```bash
# Backend: unit + integration tests, fails the build below 80% line / 75% branch coverage
cd backend && ./mvnw clean verify
# Coverage report: backend/target/site/jacoco/index.html

# Frontend
cd frontend && npm run test:coverage
# Coverage report: frontend/coverage/index.html

# Frontend linting
npm run lint
```

Backend integration tests use Testcontainers against a real `postgis/postgis` image — never an
in-memory database — because PostGIS-specific functions (`ST_Relate`, `ST_DWithin`) and GiST
indexes don't exist in generic in-memory engines, so testing against one would validate nothing
about the spatial logic that is the whole point of this project. The overlap rule is additionally
verified against plain JTS (no database at all) in a millisecond-fast domain test, so the same
business rule is proven correct at two speeds: instantly in Java, and against the real SQL that
actually runs in production.

## API reference

Base path: `/api/v1`. All geometries travel as GeoJSON (RFC 7946), EPSG:4326, `[longitude, latitude]`.

### `POST /parcels` — register a parcel

| Status | When |
|---|---|
| `201 Created` | Success — `Location` header points at the new resource |
| `400 Bad Request` | Malformed JSON/GeoJSON, missing required field |
| `422 Unprocessable Entity` | Invalid geometry (self-intersecting, too few points, unclosed ring) |
| `409 Conflict` | Boundary overlaps an existing parcel — body includes `conflictingParcelIds` |

### `POST /parcels/search` — search by circular area

Request: `{ "center": {...}, "radiusInMeters": number, "filters": { "maxPrice": number, "status": "AVAILABLE" } }`
(`filters` is optional; each field inside it is too.)

Response: `200 OK` with a GeoJSON `FeatureCollection` containing only the parcels intersecting the
circle and matching the filters, consumable directly by OpenLayers.

### `GET /parcels/{id}` — parcel detail

`200 OK` with the full parcel (unmasked contact information), or `404 Not Found`.

### `PATCH /parcels/{id}/reserve` — reserve a parcel

`200 OK` with the updated parcel, `404 Not Found`, or `409 Conflict` if the parcel isn't currently
`AVAILABLE`.

### `PATCH /parcels/{id}/sell` — mark a parcel as sold

`200 OK` with the updated parcel, or `404 Not Found`.

### `GET /actuator/health`

Health check used by the Docker Compose healthcheck chain (`db` → `api` → `web`).

## Security considerations

| Risk | Mitigation in this project |
|---|---|
| SQL injection | Every native PostGIS query uses named parameters — no string concatenation |
| Stored XSS | React escapes all rendered text by default; `innerHTML`/`dangerouslySetInnerHTML` are never used, including in the popup, which renders user-submitted descriptions |
| DoS via oversized geometry | Server-side limits on vertex count and polygon area; search radius capped |
| Information leakage | Stack traces are never returned to the client (`include-stacktrace: never`); a generic `500` body is returned while the real exception is logged server-side |
| Secrets in version control | No hardcoded database password anywhere; the Docker profile requires `DB_PASSWORD` with no fallback and fails fast at boot if it's missing |
| Race condition on overlap check | The application-level check is UX only; a `BEFORE INSERT` database trigger (migration V3) is the actual guarantee against two concurrent registrations |
| Invalid status transitions | Enforced in the domain (`LandParcel.markReserved()` throws if not `AVAILABLE`), mapped to a `409` — never a raw exception or unchecked state change |
| Containers running as root | Both Dockerfiles create and switch to a non-root user before running the application |

## Scalability notes

What already scales in this design: the GiST index on `boundary` keeps overlap checks close to
O(log n) instead of comparing against every parcel; the functional GiST index on
`boundary::geography` keeps radius search index-backed; the API is stateless, so it can run behind
a load balancer without sticky sessions; `open-in-view: false` returns pooled connections at the
end of the transaction rather than the end of the HTTP request.

Known bottlenecks and the planned evolution, in order of when they'd actually bite:

1. **Large GeoJSON payloads** as the number of listed parcels grows — short term, coordinate
   precision is already capped at 6 decimal places (~11cm) and gzip is enabled; at real scale, the
   next step would be server-side geometry simplification by zoom level, or vector tiles
   (`ST_AsMVT`) instead of raw GeoJSON.
2. **Client-side rendering** of thousands of polygons on canvas — OpenLayers' WebGL renderer, or
   moving rendering to server-generated vector tiles, would be the next step.
3. **Read-heavy traffic** (search is far more frequent than registration) — read replicas plus a
   cache keyed by a rounded search area would reduce load on the primary without touching the
   correctness guarantees above.

## Project structure

```
terramap/
├── docker-compose.yml
├── backend/                  # Spring Boot 4.1.1, hexagonal architecture
│   ├── src/main/java/com/terramap/
│   │   ├── domain/            # Business rules — zero framework dependencies
│   │   ├── application/       # Use cases and ports
│   │   ├── adapter/           # Web (REST) and persistence (JPA/PostGIS) adapters
│   │   └── config/
│   └── src/main/resources/db/migration/   # Flyway migrations V1–V3
└── frontend/                  # React + TypeScript + OpenLayers
    └── src/
        ├── api/                # Single HTTP client + typed API calls
        ├── features/map/       # OpenLayers integration (draw, search, popup)
        ├── features/parcel/    # Registration form, popup content, status updates
        ├── features/search/    # Search UI and live-radius readout
        └── shared/geo/         # The only two modules that touch projections/measurement
```

## Trade-offs and what I would do next

Written honestly rather than pretending the MVP is a finished product:

- **No authentication.** Anyone can register a parcel or change its status; there's no concept of
  an owner who exclusively controls their own listing. The domain model (`LandParcel`) and the
  database schema (`owner_id` column, currently unused) were deliberately left ready for it — the
  status-transition use case would just need to check the caller's identity against it.
- **No pagination on search results in the UI.** The API supports it; the frontend currently
  renders whatever comes back. Fine for a demo, not for a production search over a dense city.
- **Search results replace the map entirely.** This matches the specification literally ("render
  only the parcels intersecting the circle"), but a real product would likely want to blend
  "everything nearby" with "what matches my search," which needs a different state model on the
  frontend.
- **No `minAreaInSquareMeters` filter**, even though price and status are supported. Computing
  polygon area server-side for a filter is straightforward, but wasn't worth the added query
  complexity for an MVP where price and status already cover the common cases.

## License

MIT
