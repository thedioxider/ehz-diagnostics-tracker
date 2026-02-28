# App Rework Plan: New Data Model & Workflow

## Context

The app's requirements have evolved. The current structure (pre-created diagnostic points → single reports per type) needs to become a session-based workflow where sites are created on-the-go with flexible, constructor-style field composition. This plan covers the full rework: renaming, data model, config format, sessions, UI, and navigation.

## Naming Convention

| Old (code)         | New (code) | Russian UI term          |
| ------------------ | ---------- | ------------------------ |
| `DiagnosticObject` | `Route`    | объект диагностирования  |
| `DiagnosticArea`   | `Section`  | участок диагностирования |
| `DiagnosticPoint`  | `Site`     | точка диагностирования   |

---

## Phase 1: Rename Entities & Config Restructure ✅

### 1a. Rename database entities

Rename classes and table names. Since the app uses `fallbackToDestructiveMigration`, we can simply rename everything and bump the version — no manual migration needed.

**`data/ReportDatabase.kt`**:

- `DiagnosticObject` → `Route` (table: `routes`)
- `DiagnosticArea` → `Section` (table: `sections`, FK field: `routeId`)
- `DiagnosticObjectWithAreas` → `RouteWithSections`
- `DiagnosticAreaWithPoints` → remove (points no longer pre-exist)
- `DiagnosticAreaDao` → `RouteDao` (rename queries accordingly)
- Remove `DiagnosticPoint`, `DiagnosticPointWithHistory` — points are now session-created sites
- Remove `DiagnosticReport`, `ReportDataEntry` — replaced by new session/site schema (Phase 2)
- Fix `Coordinates.longitude` column name from `"altitude"` to `"longitude"`

**`data/DiagnosticSession.kt`**: Update to use `Route` and `Section` types. Rename to `ReportSession` deferred to Phase 2 (name collision with old `ReportSession.kt`).

**`data/ReportConfig.kt`**:

- `InitialAreaEntries` → rename fields: `routes`, `sections` (drop `diagnosticPoints`)

**`data/AppState.kt`**: Update references to new entity names.

**`data/CompositionLocals.kt`**: Rename `LocalDiagnosticSession` deferred to Phase 2 (together with `DiagnosticSession` → `ReportSession`).

All screens and `AppNavigation.kt`: update references. Use IDE rename refactoring where possible.

### 1b. Restructure YAML config — **deferred to Phase 2**

Class renames done (`ParameterType` → `FieldType`, `ReportParameter` → `FieldSpec`, `ReportType` → `ReportTemplate`, `reportTypes` → `templates`). The full structural change (flat `field_specs` list + `templates` with `spec_ids` string references, deduplication, new YAML format) is deferred to Phase 2 since it's tightly coupled with the new data model.

### 1c. Update initial data YAML

Updated field names in `initial_area_entries.yaml` (`diagnostic_object` → `route_id`, `diagnostic_area` → `section_id`). Top-level keys kept as-is (matching `InitialAreaEntries` SerialNames).

**Files touched**: `ReportDatabase.kt`, `ReportConfig.kt`, `DiagnosticSession.kt` (→ rename), `AppState.kt`, `CompositionLocals.kt`, `AppNavigation.kt`, all screens, `report_conf.yaml`, `initial_area_entries.yaml`, `ReportExporter.kt`

**Verification**: App compiles, routes/sections display correctly in selection screen, config loads.

---

## Phase 2: New Session & Site Data Model

### 2a. Revisit Route and Section entities

The `Route` and `Section` classes inherited from Phase 1 likely have redundant fields from the old schema (e.g. `inventoryId`, `registrationNumber`, `dangerClass`, `additionalInfo`). Review and trim them to only what the new workflow needs before adding new entities.

### 2b. Restructure YAML config (deferred from Phase 1)

Change `report_conf.yaml` from report-type-centric to a flat parameter library + templates:
- `field_specs`: flat list of all field definitions (deduplicated across all report types)
- `templates`: named groups referencing spec IDs (strings, not embedded objects)
- Update `ReportConfig` to have `fieldSpecs: List<FieldSpec>` + `templates: List<ReportTemplate>`
- `ReportTemplate.specIds` becomes `List<String>` (references) instead of `List<FieldSpec>` (embedded)
- Add `ReportConfig.resolveTemplate(template: ReportTemplate): List<FieldSpec>` helper

### 2c. New database entities

Add to `ReportDatabase.kt`:

```kotlin
@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String,       // UUID
    val routeId: String,
    val sectionId: String,
    val inspectorName: String,
    val startTime: Long,              // epoch seconds
    val endTime: Long?,               // null while active
    val isComplete: Boolean = false
)
// Initial point (zero-km) is just the first site in the report (earliest timestamp).
// No need to store its data separately in Report.

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey val id: String,       // UUID
    val reportId: String,             // FK to Report
    val positionKm: Double?,          // km on route, auto-calculated, editable
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val validationId: String?,        // QR scan result — non-null means verified
    val locationOnly: Boolean = false // true = location marker (no fields, only position editable)
                                      // false = measurement site (fields can be added/edited)
)
// locationOnly distinguishes "intentionally location-only" from
// "measurement site with no fields yet". Location-only sites have
// a separate editor and only their position is editable.

@Entity(tableName = "report_fields", primaryKeys = ["siteId", "specId"])
data class ReportField(
    val siteId: String,               // FK to Site
    val specId: String,               // references FieldSpec.id
    val name: String,                 // copied from FieldSpec.name at creation
    val type: String,                 // copied from FieldSpec.type at creation
    val unit: String?,                // copied from FieldSpec.unit at creation
    val value: String
)
// Spec metadata is baked in at creation time — reports are self-contained
// and readable even if the original YAML config is lost.
```

**Relations** (nested — one query loads the full tree):

- `SiteWithFields` — site + its field entries
- `FullReport` — report + `List<SiteWithFields>`

**New DAO**: `ReportDao` with queries for CRUD on reports, sites, and fields. Key queries:

- Active (incomplete) report for a section
- All completed reports for a section (history)
- Sites ordered by `timestamp` within a report
- Fields for a site

### 2d. Session state management

**`data/SessionManager.kt`** (new): Facade over `ReportDao`. No in-memory state duplication — all data lives in Room, read reactively via `Flow`.

```kotlin
class SessionManager(private val dao: ReportDao) {
    // Current report ID, set by startReport() or resumeReport()
    var activeReportId: String? = null
        private set

    // Live data from Room — collected as Compose state in UI
    val fullReport: Flow<FullReport?>
        get() = activeReportId?.let { dao.getFullReport(it) } ?: flowOf(null)

    fun startReport(route: Route, section: Section, inspector: Profile): String
    fun resumeReport(reportId: String)
    fun addSite(coordinates: Coordinates?, locationOnly: Boolean = false): Site
    fun updateSitePosition(siteId: String, positionKm: Double)
    fun addFieldToSite(siteId: String, specId: String)
    fun applyTemplateToSite(siteId: String, template: ReportTemplate)
    fun updateFieldValue(siteId: String, specId: String, value: String)
    fun removeSite(siteId: String)
    fun removeFieldFromSite(siteId: String, specId: String)
    fun completeReport()
    fun calculatePosition(coordinates: Coordinates): Double
}
```

How it works:

- `SessionManager` only holds `activeReportId` — no copies of sites/fields in memory
- `fullReport` is a `Flow<FullReport?>` from Room, which emits a new value whenever any related table changes
- UI collects this flow (e.g. `val report by sessionManager.fullReport.collectAsState()`) and recomposes automatically
- Every mutation method (`addSite`, `updateFieldValue`, etc.) writes to Room via DAO — Room triggers flow re-emission → UI updates
- Auto-save is inherent: data is always in the database, never only in memory

### 2e. Remove old entities

Drop `DiagnosticPoint`, `DiagnosticReport`, `ReportDataEntry`, `DiagnosticPointWithHistory`, `DiagnosticReportWithData`, `DiagnosticReportDao`. Remove old `ReportSession.kt` and rename `DiagnosticSession` → `ReportSession` (+ `LocalDiagnosticSession` → `LocalReportSession`).

**Files touched**: `ReportDatabase.kt`, new `SessionManager.kt`, delete `ReportSession.kt`

**Verification**: Can create a session, add sites with fields, persist and reload.

---

## Phase 3: Navigation & Screen Rework

### 3a. New navigation flow

```
Authorization → RouteSelection (choose route → section)
  ↓
  ├─ View history → ReportList (filter by route or section)
  │     └─ Report → SiteEditor / LocationSiteEditor
  └─ Start/resume session:
       Check for active session on this section:
         → If exists: SessionHub (resume)
         → If none: LocationSiteEditor (create initial point) → SessionHub
             ↓
           SiteEditor (measurement site) or LocationSiteEditor (location-only site)
```

`ReportScreen` and `SiteEditorScreen` are universal — used for both active sessions and saved reports.

**Screen sealed class** update:

```kotlin
sealed class Screen(val route: String) {
    object Initial : Screen("initial")
    object OutOfService : Screen("out_of_service")
    object Authorization : Screen("authorization")
    object ProfileEdit : Screen("profile_edit")
    object RouteSelection : Screen("route_selection")
    object Settings : Screen("settings")
    object ReportList : Screen("report_list")
    object Report : Screen("report")               // universal: site list for any report
    object SiteEditor : Screen("site_editor")           // measurement site editor (preview/edit)
    object LocationSiteEditor : Screen("location_site_editor") // location-only site editor
    object SessionHub : Screen("session_hub")           // control panel for active session
}
```

### 3b. Screen changes

| Old screen                | Action          | New screen                                                                 |
| ------------------------- | --------------- | -------------------------------------------------------------------------- |
| `DiagnosticObjectsScreen` | Rename + Rework | `RouteSelectionScreen` — pick route then section                           |
| `DiagnosticMenuScreen`    | Rework          | `SessionHubScreen` — control panel for active diagnosis session            |
| `DiagnosticPointsScreen`  | Delete          | —                                                                          |
| `ReportEditorScreen`      | Rename + Rework | `SiteEditorScreen` — measurement site editor (preview/edit modes)          |
| `ReportHistoryScreen`     | Rename + Rework | `ReportListScreen` — completed reports, filterable by route or section     |
| —                         | New             | `ReportScreen` — universal: site list for any report (active or completed) |
| —                         | New             | `LocationSiteEditorScreen` — editor for location-only sites                |

### 3c. Screen descriptions

**`RouteSelectionScreen`**:
- Pick route, then section
- View report history for selected route/section → `ReportListScreen`
- Start/resume session → `SessionHubScreen` or `LocationSiteEditorScreen` (initial point) → `SessionHubScreen`

**`SessionHubScreen`** (control panel for active diagnosis):
- Session info header (route, section, inspector, start time)
- Create new measurement site → `SiteEditorScreen`
- Create new location-only site → `LocationSiteEditorScreen`
- View current report → `ReportScreen` (sites saved in this session)
- Complete session → marks report done, navigates to `ReportListScreen`
- Open last site on map

**`ReportListScreen`**:
- Shows completed reports, filtered by route or by specific section
- Each item shows: section name, inspector, date, site count
- Click → `ReportScreen`
- Export button per report
- Bulk export: select multiple reports → export all at once

**`ReportScreen`** (universal — active session and saved reports):
- LazyColumn of sites (ordered by timestamp, showing position + field count)
- Click measurement site → `SiteEditorScreen` (opens in preview mode)
- Click location-only site → `LocationSiteEditorScreen`
- Export button

**`SiteEditorScreen`** (measurement sites only):
- Two modes: **preview** (read-only, with edit button to switch) and **edit**
- Preview mode: displays site data — position, coordinates, fields with values
- Edit mode:
  - Position display (auto-calculated, editable)
  - GPS coordinates (auto-captured, refreshable)
  - QR scan / NFC scan (not implemented yet) button
    - launches an activity and sets validationId to its result
  - "Insert template" button → `TemplateChooserDialog`
  - "Add field" button → `FieldSpecChooserDialog` with search
  - List of current fields — each rendered by type (text input, decimal input, bool toggle, photo capture)
  - "Manage fields" toggle — shows delete icons on each field, hidden otherwise
  - Save button (writes to Room via SessionManager)
- When creating a new site (from SessionHub), opens directly in edit mode

**`LocationSiteEditorScreen`** (location-only sites):
- GPS capture for current position (auto-captured, refreshable)
- Position on route in km (editable)
- Save button
- Used for: initial point (zero-km) when starting a session, and any location-only markers during a session

**`FieldSpecChooserDialog`** (new in `ui/`):
- Search field at top
- Scrollable list of all field specs from config
- Already-added specs greyed out
- Click to add

**`TemplateChooserDialog`** (new in `ui/`):
- List of templates from config
- Click to insert all template's field specs into site (skipping duplicates)

### 3d. Update `AppNavigation.kt`

- Replace `DiagnosticSession` with `ReportSession` (route + section only)
- Provide `SessionManager` via CompositionLocal or pass as parameter
- Wire new screen routes

**Files touched**: `AppNavigation.kt`, rename/rework 4 screens, create 3 new screens, create 2 new dialogs, delete `DiagnosticPointsScreen.kt`

**Verification**: Full flow works — login → select route/section → set initial point → create sites → add fields via template and individually → complete session. View completed reports from history, edit sites in saved reports.

---

## Phase 4: Export & History

### 4a. Update export format

**`data/ReportExporter.kt`**: Export a completed session as JSON.

```json
{
  "version": 2,
  "route": { "id": "...", "name": "..." },
  "section": { "id": "...", "name": "..." },
  "inspector": "...",
  "start_time": 1234567890,
  "end_time": 1234567890,
  "sites": [
    // first site is the initial point (zero-km)
    {
      "id": "uuid",
      // "location_only": true — only present when true, omitted otherwise
      "position_km": 24.1,
      "latitude": 55.001,
      "longitude": 37.001,
      "timestamp": 1234567890,
      "validation_id": null,
      "fields": [
        {
          "spec_id": "...",
          "name": "...",
          "value": "123",
          "type": "decimal",
          "unit": "В"
        }
      ]
    }
  ]
}
```

### 4b. History & export integration

`ReportListScreen` and `ReportScreen` (already defined in Phase 3) handle viewing. This phase wires up the export logic to those screens.

**Files touched**: `ReportExporter.kt`, `ReportListScreen.kt`, `ReportScreen.kt`

**Verification**: Complete a session → view in `ReportListScreen` → open report → export to JSON → verify JSON structure. Test bulk export.

---

## Phase 5: Cleanup

- Remove all old `Diagnostic*` prefixed classes and dead code
- Update `strings.xml` with any new Russian strings
- Remove unused imports and files
- Ensure `initial_area_entries.yaml` matches new schema
- Final build + test

---

## Implementation Notes

- **Database migration**: Destructive is fine (app already uses `fallbackToDestructiveMigration`). No need to preserve old data.
- **IDE refactoring**: For bulk renames (e.g. `DiagnosticObject` → `Route`), the plan will provide instructions to use Android Studio's Rename Symbol refactoring where appropriate, rather than editing every file manually.
- **Incremental**: Each phase produces a compilable app. Phase 1 is the largest (touches most files) but is mostly mechanical renaming.
