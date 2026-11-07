# EHZ Diagnostics Tracker

Android app for field diagnostics of gas pipeline electrochemical protection. A technician walks a pipeline section, records measurements at diagnostic sites (GPS position, kilometer mark, photos, typed fields), and exports the finished report as JSON or syncs it to [`ehz-backend`](#backend-sync) over the LAN.

## Features

- Route -> section -> site hierarchy; routes and sections persist, sites are per-report
- Sessions auto-save on every change and can be resumed after the app is closed
- Site position calculated from GPS displacement relative to the previous site (can be edited)
- Measurement fields defined by a YAML config; add them one by one or insert a template
- In-app camera and gallery picker for photos; QR scan to verify a site
- Local user profiles with password login, plus a separate admin password
- Report history with filtering and bulk delete
- JSON file export and LAN server sync (auto-discovery, bearer auth)

## Domain model

| Entity      | Meaning                                                                                                                                                                                         |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Route**   | Whole pipeline (up to hundreds of km)                                                                                                                                                           |
| **Section** | Segment of a route, named by start/end km                                                                                                                                                       |
| **Report**  | One diagnostic session on a section: an ordered list of sites                                                                                                                                   |
| **Site**    | Measurement point: km position, GPS coordinates, fields. Can be location-only (e.g. the zero-km initial point). Starts as a draft until confirmed; counts as verified once a QR code is scanned |
| **Field**   | Typed value on a site, created from a `FieldSpec` in the config                                                                                                                                 |

## Requirements

- Android 8.0+ (API 26), Google Play Services (for location)
- Build: JDK 17+, Android SDK 36

## Building

The build script reads release signing keys from `local.properties` for **every** build type, so it must contain:

```properties
sdk.dir=/path/to/Android/Sdk
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/EHZTracker-v<version>-debug.apk
./gradlew assembleRelease   # R8 + resource shrinking; EHZTracker-v<version>.apk
./gradlew test              # unit tests
```

## Report config

Field definitions come from a YAML file. The bundled default is `app/src/main/res/raw/report_conf.yaml`. To replace it on a device, go to **Настройки -> Конфигурация отчётов** (Settings -> Report configuration); the imported file is copied into the app's data directory and used from then on.

```yaml
field_specs:
  - id: comment # meaningful name or UUID; must be unique
    name: Комментарий
    type: text # text | bool | integer | decimal | photo | raw | enum
  - id: c7fed2e2-b7e8-4e9a-aaa2-08ded2710f8a
    name: Сопротивление растеканию тока защитного заземления
    type: decimal
    unit: Ом # optional
  - id: soil_type
    name: Тип грунта
    type: enum
    values: # required for enum, forbidden otherwise
      - { code: clay, label: "Глина" }
      - { code: sand, label: "Песок" }

default_specs: [site_photo, comment] # added to every new site

templates:
  - id: skz
    name: СКЗ
    spec_ids: [c7fed2e2-b7e8-4e9a-aaa2-08ded2710f8a, comment]
```

## Export format

Completed reports are exported as a single JSON file (`reports_<timestamp>.json`), saved through the system file picker. IDs are UUIDs and timestamps are ISO-8601 strings. Fields equal to their default value are omitted. Server sync uses the same DTOs (`api/Dto.kt`).

## Backend sync

The export screen can upload selected reports to `ehz-backend`:

1. **Discovery**: mDNS `_ehz-backend._tcp` (2 s timeout), then UDP broadcast on port `5370`. If neither finds the backend, the URL from Settings is used.
2. **Upload**: `PUT /api/reports/{id}` with `Authorization: Bearer <key>`, one report at a time. Every response must carry `X-Contract-Version: 1`, otherwise sync stops with a version-mismatch message.
3. **Errors**:
   - 401: the app asks for a new bearer key, then resumes from the report that failed.
   - 404: sync stops with a "server not found" message. On its LAN port the backend returns 404 for every path outside its allowlist (e.g. a report id that isn't a UUID).
   - Other HTTP and network errors mark that report as failed and sync moves on to the next one.

The bearer key and the fallback URL are set in **Настройки** (Settings).

## Tech stack

Kotlin, Jetpack Compose (Material 3, single activity, no ViewModels), Navigation Compose, Room, DataStore, kotlinx.serialization + kaml, CameraX + ML Kit barcode scanning, Play Services Location, Ktor CIO.

## Project layout

```
app/src/main/java/space/diomentia/ehzdiagnosticstracker/
  AppNavigation.kt   NavHost and routes
  *Activity.kt       main, photo capture, QR scan
  api/               backend client, LAN discovery, wire DTOs
  data/              Room DBs, SessionManager, config model, exporter, preferences
  ui/                screens, theme, shared composables
app/schemas/         exported Room schemas
```
