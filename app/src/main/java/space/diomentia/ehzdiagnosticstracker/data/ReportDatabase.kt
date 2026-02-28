package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Database(
    version = 7,
    entities = [
        DiagnosticReport::class,
        ReportDataEntry::class,
        Route::class,
        Section::class,
        DiagnosticPoint::class
    ]
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun diagnosticReportDao(): DiagnosticReportDao
    abstract fun routeDao(): RouteDao

    companion object {
        const val DB_NAME = "report-db"

        @Volatile
        private var INSTANCE: ReportDatabase? = null

        fun getInstance(context: Context) = synchronized(this) {
            INSTANCE ?: Room
                .databaseBuilder(context, ReportDatabase::class.java, DB_NAME)
                // TODO: get rid of this crutch
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Composable
        fun getInstance() = getInstance(LocalContext.current)
    }
}

@Serializable
data class Coordinates(
    @ColumnInfo(name = "latitude") @SerialName("latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude") @SerialName("longitude")
    val longitude: Double
)

@Entity(
    tableName = "diagnostic_reports",
    indices = [
        Index("diagnostic_point", "timestamp", unique = true, name = "report_id")
    ]
)
data class DiagnosticReport(
    @ColumnInfo(name = "diagnostic_point") @SerialName("diagnostic_point")
    val diagnosticPoint: String,
    @ColumnInfo(name = "timestamp") @SerialName("timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "diagnostics_type") @SerialName("diagnostics_type")
    val diagnosticsType: String,
    @ColumnInfo(name = "report_type") @SerialName("report_type")
    val reportType: String,
    @Embedded @SerialName("location")
    val location: Coordinates,
    @ColumnInfo(name = "inspector") @SerialName("inspector")
    val inspector: String,
    /**
     * A validation ID is known when a report point is scanned (via QR or NFC). Can be used for
     * verification of indeed reaching the point and for identification the same point on server side.
     */
    @ColumnInfo(name = "validation_id") @SerialName("validation_id")
    val validationId: String? = null,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") @SerialName("id")
    val id: Long = 0
)

@Entity(tableName = "data_entries", primaryKeys = ["report", "characteristic"])
data class ReportDataEntry(
    @ColumnInfo(name = "report") @SerialName("report")
    val report: Long,
    @ColumnInfo(name = "characteristic") @SerialName("characteristic")
    val characteristic: String,
    @ColumnInfo(name = "value") @SerialName("value")
    val value: String,
    @ColumnInfo(name = "datatype") @SerialName("datatype")
    val datatype: String? = null
)

data class DiagnosticReportWithData(
    @Embedded val report: DiagnosticReport,
    @Relation(parentColumn = "id", entityColumn = "report") val data: List<ReportDataEntry>
)

@Dao
interface DiagnosticReportDao {
    @Transaction
    @Query("SELECT * FROM diagnostic_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<DiagnosticReportWithData>>

    @Transaction
    @Query("SELECT * FROM diagnostic_reports WHERE id = :id")
    fun getReportById(id: Long): Flow<DiagnosticReportWithData>

    @Transaction
    @Query("SELECT * FROM diagnostic_reports WHERE id = :id")
    fun getReportByIdOnce(id: Long): DiagnosticReport?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertReport(report: DiagnosticReport): Long

    @Update
    fun updateReport(report: DiagnosticReport)

    @Query("SELECT * FROM data_entries WHERE report = :report")
    fun getReportData(report: Long): Flow<List<ReportDataEntry>>

    @Query("SELECT * FROM data_entries WHERE report = :report")
    fun getReportDataOnce(report: Long): List<ReportDataEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertData(vararg data: ReportDataEntry)

    @Delete
    fun deleteReport(report: DiagnosticReport)

    @Delete
    fun deleteData(vararg data: ReportDataEntry)
}

@Serializable
@Entity(tableName = "routes")
data class Route(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "inventory_id") @SerialName("inventory_id")
    val inventoryId: String,
    @ColumnInfo(name = "name") @SerialName("name")
    val name: String,
    @ColumnInfo(name = "additional_info") @SerialName("additional_info")
    val additionalInfo: String? = null
) {
    companion object {
        fun checkId(id: String): Boolean = id.all { c -> c.isLetterOrDigit() }
        fun checkName(name: String): Boolean =
            name.all { c -> c.isLetterOrDigit() || c in ",.;:()" }
    }
}

@Serializable
@Entity(tableName = "sections")
data class Section(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "name") @SerialName("name")
    val name: String,
    @ColumnInfo(name = "route_id") @SerialName("route_id")
    val routeId: String,
    @ColumnInfo(name = "danger_class") @SerialName("danger_class")
    val dangerClass: String,
    @Embedded(prefix = "last_") @SerialName("last_location")
    val lastLocation: Coordinates? = null,
    @ColumnInfo(name = "additional_info") @SerialName("additional_info")
    val additionalInfo: String? = null
) {
    companion object {
        fun checkId(id: String): Boolean = id.all { c -> c.isLetterOrDigit() }
        fun checkName(name: String): Boolean =
            name.all { c -> c.isLetterOrDigit() || c in ",.;:()" }
    }
}

@Serializable
@Entity(tableName = "diagnostic_points")
data class DiagnosticPoint(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "section_id") @SerialName("section_id")
    val sectionId: String,
    @ColumnInfo(name = "inventory_id") @SerialName("inventory_id")
    val inventoryId: String,
    @ColumnInfo(name = "registration_number") @SerialName("registration_number")
    val registrationNumber: String,
    @ColumnInfo(name = "diagnostics_type") @SerialName("diagnostics_type")
    val diagnosticsType: String? = null,
    @ColumnInfo(name = "additional_info") @SerialName("additional_info")
    val additionalInfo: String? = null
) {
    companion object {
        fun checkId(id: String): Boolean = id.all { c -> c.isLetterOrDigit() }
        fun checkName(name: String): Boolean =
            name.all { c -> c.isLetterOrDigit() || c in ",.;:()" }
    }
}

data class RouteWithSections(
    @Embedded val route: Route,
    @Relation(
        parentColumn = "id",
        entityColumn = "route_id"
    ) val sections: List<Section>
)

data class DiagnosticPointWithHistory(
    @Embedded val point: DiagnosticPoint,
    @Relation(
        parentColumn = "id",
        entityColumn = "diagnostic_point"
    ) val history: List<DiagnosticReport>
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

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE id = :id")
    fun getPointById(id: String): Flow<DiagnosticPointWithHistory>

    @Query("SELECT * FROM diagnostic_points WHERE id = :id")
    fun getPointByIdOnce(id: String): DiagnosticPoint?

    @Query("SELECT * FROM sections WHERE route_id = :routeId ORDER BY id ASC")
    fun getRouteSections(routeId: String): Flow<List<Section>>

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE section_id = :sectionId ORDER BY id ASC")
    fun getSectionPoints(sectionId: String): Flow<List<DiagnosticPointWithHistory>>

    @Transaction
    @Query("SELECT * FROM diagnostic_reports WHERE diagnostic_point = :diagnosticPoint ORDER BY id ASC")
    fun getPointReports(diagnosticPoint: String): Flow<List<DiagnosticReportWithData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRoute(route: Route)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSection(section: Section)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPoint(diagnosticPoint: DiagnosticPoint)

    @Update
    fun updateRoute(route: Route)

    @Update
    fun updateSection(area: Section)

    @Update
    fun updatePoint(point: DiagnosticPoint)

    @Delete
    fun deleteRoute(route: Route)

    @Delete
    fun deleteSection(area: Section)

    @Delete
    fun deletePoint(point: DiagnosticPoint)
}