package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.diomentia.ehzdiagnosticstracker.R
import java.time.Instant

// WARNING: fallbackToDestructiveMigration wipes all data on version bump.
// Proper migrations MUST be written for production releases.
@Database(
    version = 18,
    entities = [
        Route::class,
        Section::class,
        Report::class,
        Site::class,
        ReportField::class
    ]
)
@TypeConverters(InstantConverter::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun reportDao(): ReportDao

    companion object {
        const val DB_NAME = "report-db"

        private val MIGRATION_17_18 = Migration(17, 18) { db ->
            db.execSQL(
                "UPDATE report_fields SET value = REPLACE(value, ',', '.') WHERE LOWER(type) = 'decimal'"
            )
            db.execSQL("UPDATE report_fields SET type = LOWER(type)")
        }

        @Volatile
        private var INSTANCE: ReportDatabase? = null

        fun getInstance(context: Context) = synchronized(this) {
            INSTANCE ?: Room
                .databaseBuilder(context, ReportDatabase::class.java, DB_NAME)
                // TODO: get rid of this crutch
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_17_18)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Composable
        fun getInstance() = getInstance(LocalContext.current)
    }
}

@Serializable
@Entity(tableName = "routes")
data class Route(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "name") @SerialName("name")
    val name: String,
    @ColumnInfo(name = "length_km") @SerialName("length_km")
    val lengthKm: Int
) {
    companion object {
        fun checkId(id: String): Boolean = isCanonicalUuid(id)
        fun checkName(name: String): Boolean =
            name.all { c -> c.isLetterOrDigit() || c in " ,.;:()-" }
    }
}

@Serializable
@Entity(tableName = "sections")
data class Section(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "begin_km") @SerialName("begin_km")
    val beginKm: Int,
    @ColumnInfo(name = "end_km") @SerialName("end_km")
    val endKm: Int,
    @ColumnInfo(name = "route_id") @SerialName("route_id")
    val routeId: String
) {
    @Composable
    fun displayName(): String = stringResource(R.string.section_display_name, beginKm, endKm)

    companion object {
        fun checkId(id: String): Boolean = isCanonicalUuid(id)
    }
}

data class RouteWithSections(
    @Embedded val info: Route,
    @Relation(
        parentColumn = "id",
        entityColumn = "route_id"
    ) val sections: List<Section>
)

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes ORDER BY name ASC")
    fun getAllRoutes(): Flow<List<RouteWithSections>>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :id")
    fun getRouteById(id: String): Flow<RouteWithSections>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getRouteByIdOnce(id: String): Route?

    @Query("SELECT * FROM sections WHERE id = :id")
    fun getSectionById(id: String): Flow<Section>

    @Query("SELECT * FROM sections WHERE id = :id")
    fun getSectionByIdOnce(id: String): Section?

    @Query("SELECT * FROM sections WHERE route_id = :routeId ORDER BY id ASC")
    fun getRouteSections(routeId: String): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRoute(route: Route)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSection(section: Section)

    @Update
    fun updateRoute(route: Route)

    @Update
    fun updateSection(area: Section)

    @Delete
    fun deleteRoute(route: Route): Int

    @Delete
    fun deleteSection(area: Section): Int
}

// --- Session-based entities ---

@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Section::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Report(
    @PrimaryKey @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "route_id")
    val routeId: String,
    @ColumnInfo(name = "section_id")
    val sectionId: String,
    @ColumnInfo(name = "inspector_name")
    val inspectorName: String,
    @ColumnInfo(name = "start_time")
    val startTime: Instant,
    @ColumnInfo(name = "end_time")
    val endTime: Instant? = null,
    @ColumnInfo(name = "is_complete")
    val isComplete: Boolean = false
)

@Serializable
data class Coordinates(
    @ColumnInfo(name = "latitude") @SerialName("lat")
    val lat: Double,
    @ColumnInfo(name = "longitude") @SerialName("lng")
    val lng: Double
)

@Entity(
    tableName = "sites",
    foreignKeys = [ForeignKey(
        entity = Report::class,
        parentColumns = ["id"],
        childColumns = ["report_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Site(
    @PrimaryKey @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "report_id")
    val reportId: String,
    @ColumnInfo(name = "position_km")
    val positionKm: Double? = null,
    @Embedded
    val location: Coordinates? = null,
    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,
    @ColumnInfo(name = "validation_id")
    val validationId: String? = null,
    @ColumnInfo(name = "location_only")
    val locationOnly: Boolean = false,
    @ColumnInfo(name = "is_draft")
    val isDraft: Boolean = true
)

@Entity(
    tableName = "report_fields",
    primaryKeys = ["site_id", "spec_id"],
    foreignKeys = [ForeignKey(
        entity = Site::class,
        parentColumns = ["id"],
        childColumns = ["site_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ReportField(
    @ColumnInfo(name = "site_id")
    val siteId: String,
    @ColumnInfo(name = "spec_id")
    val specId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "unit")
    val unit: String? = null,
    @ColumnInfo(name = "value")
    val value: String = "",
    @ColumnInfo(name = "value_label")
    val valueLabel: String? = null
)

data class SiteWithFields(
    @Embedded val info: Site,
    @Relation(parentColumn = "id", entityColumn = "site_id")
    val fields: List<ReportField>
)

data class FullReport(
    @Embedded val info: Report,
    @Relation(
        entity = Site::class,
        parentColumn = "id",
        entityColumn = "report_id"
    )
    val sites: List<SiteWithFields>,
    @Relation(
        parentColumn = "route_id",
        entityColumn = "id"
    )
    val route: Route,
    @Relation(
        parentColumn = "section_id",
        entityColumn = "id"
    )
    val section: Section
)

@Dao
interface ReportDao {
    @Transaction
    @Query("SELECT * FROM reports WHERE id = :id")
    fun getFullReport(id: String): Flow<FullReport?>

    @Transaction
    @Query("SELECT * FROM reports WHERE id IN (:reportIds)")
    fun getFullReportsByIdsOnce(reportIds: List<String>): List<FullReport>

    @Transaction
    @Query("SELECT * FROM reports WHERE section_id = :sectionId AND is_complete = 0 LIMIT 1")
    fun getActiveReportForSection(sectionId: String): Flow<FullReport?>

    @Transaction
    @Query("SELECT * FROM reports WHERE section_id = :sectionId AND is_complete = 0 LIMIT 1")
    fun getActiveReportForSectionOnce(sectionId: String): FullReport?

    @Transaction
    @Query("SELECT * FROM reports WHERE section_id = :sectionId AND is_complete = 1 ORDER BY start_time DESC")
    fun getCompletedReportsForSection(sectionId: String): Flow<List<FullReport>>

    @Transaction
    @Query("SELECT * FROM reports WHERE route_id = :routeId AND is_complete = 1 ORDER BY start_time DESC")
    fun getCompletedReportsForRoute(routeId: String): Flow<List<FullReport>>

    @Transaction
    @Query("SELECT * FROM reports WHERE is_complete = 1 ORDER BY start_time DESC")
    fun getAllCompletedReports(): Flow<List<FullReport>>

    @Query("SELECT * FROM sites WHERE report_id = :reportId ORDER BY timestamp ASC")
    fun getSitesForReport(reportId: String): Flow<List<Site>>


    @Query("SELECT * FROM sites WHERE report_id = :reportId ORDER BY timestamp ASC")
    fun getSitesForReportOnce(reportId: String): List<Site>

    @Transaction
    @Query("SELECT * FROM sites WHERE report_id = :reportId ORDER BY timestamp ASC")
    fun getSitesWithFieldsForReport(reportId: String): Flow<List<SiteWithFields>>

    @Query("SELECT * FROM reports WHERE id = :id")
    fun getReportByIdOnce(id: String): Report?

    @Transaction
    @Query("SELECT * FROM sites WHERE id = :id")
    fun getSiteWithFields(id: String): Flow<SiteWithFields?>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun getSiteByIdOnce(id: String): Site?

    @Transaction
    @Query("SELECT * FROM sites WHERE report_id = :reportId AND is_draft = 1 LIMIT 1")
    fun getDraftSiteForReport(reportId: String): SiteWithFields?

    @Query("DELETE FROM sites WHERE report_id = :reportId AND is_draft = 1")
    fun deleteDraftSiteForReport(reportId: String)

    @Query("SELECT * FROM report_fields WHERE site_id = :siteId")
    fun getFieldsForSite(siteId: String): Flow<List<ReportField>>

    @Query("SELECT * FROM report_fields WHERE site_id = :siteId")
    fun getFieldsForSiteOnce(siteId: String): List<ReportField>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertReport(report: Report)

    @Update
    fun updateReport(report: Report)

    @Delete
    fun deleteReport(report: Report)

    @Query("DELETE FROM reports WHERE id = :reportId")
    fun deleteReportById(reportId: String): Int

    @Query("DELETE FROM reports WHERE id IN (:reportIds)")
    fun deleteReportsByIds(reportIds: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertSite(site: Site)

    @Update
    fun updateSite(site: Site)

    @Delete
    fun deleteSite(site: Site)

    @Query("DELETE FROM sites WHERE id = :siteId")
    fun deleteSiteById(siteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertField(field: ReportField)

    fun insertFieldFromConfig(siteId: String, specId: String, config: ReportConfig): Boolean {
        val spec = config.getSpec(specId) ?: return false
        insertField(
            ReportField(
                siteId = siteId,
                specId = specId,
                name = spec.name,
                type = spec.type.datatype,
                unit = spec.unit
            )
        )
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFields(fields: List<ReportField>)

    @Delete
    fun deleteField(field: ReportField)

    @Delete
    fun deleteFields(fields: List<ReportField>)

    @Query("DELETE FROM report_fields WHERE site_id = :siteId AND spec_id = :specId")
    fun deleteFieldByKey(siteId: String, specId: String)
}