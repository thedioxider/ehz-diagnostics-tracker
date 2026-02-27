package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.AutoMigration
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
    version = 6,
    entities = [
        DiagnosticReport::class,
        ReportDataEntry::class,
        DiagnosticObject::class,
        DiagnosticArea::class,
        DiagnosticPoint::class
    ],
    autoMigrations = [
        AutoMigration(from = 3, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun diagnosticReportDao(): DiagnosticReportDao
    abstract fun diagnosticAreaDao(): DiagnosticAreaDao

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
    @ColumnInfo(name = "altitude") @SerialName("altitude")
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
@Entity(tableName = "diagnostic_objects")
data class DiagnosticObject(
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
@Entity(tableName = "diagnostic_areas")
data class DiagnosticArea(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id")
    val id: String,
    @ColumnInfo(name = "name") @SerialName("name")
    val name: String,
    @ColumnInfo(name = "diagnostic_object") @SerialName("diagnostic_object")
    val diagnosticObject: String,
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
    @ColumnInfo(name = "diagnostic_area") @SerialName("diagnostic_area")
    val diagnosticArea: String,
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

data class DiagnosticObjectWithAreas(
    @Embedded val diagnosticObject: DiagnosticObject,
    @Relation(
        parentColumn = "id",
        entityColumn = "diagnostic_object"
    ) val areas: List<DiagnosticArea>
)

data class DiagnosticAreaWithPoints(
    @Embedded val diagnosticArea: DiagnosticArea,
    @Relation(
        parentColumn = "id",
        entityColumn = "diagnostic_area"
    ) val points: List<DiagnosticPoint>
)


data class DiagnosticPointWithHistory(
    @Embedded val point: DiagnosticPoint,
    @Relation(
        parentColumn = "id",
        entityColumn = "diagnostic_point"
    ) val history: List<DiagnosticReport>
)

@Dao
interface DiagnosticAreaDao {
    @Transaction
    @Query("SELECT * FROM diagnostic_objects ORDER BY name ASC")
    fun getAllObjects(): Flow<List<DiagnosticObjectWithAreas>>

    @Transaction
    @Query("SELECT * FROM diagnostic_objects WHERE id = :id")
    fun getObjectById(id: String): Flow<DiagnosticObjectWithAreas>

    @Query("SELECT * FROM diagnostic_objects WHERE id = :id")
    fun getObjectByIdOnce(id: String): DiagnosticObject?

    @Transaction
    @Query("SELECT * FROM diagnostic_areas WHERE id = :id")
    fun getAreaById(id: String): Flow<DiagnosticAreaWithPoints>

    @Query("SELECT * FROM diagnostic_areas WHERE id = :id")
    fun getAreaByIdOnce(id: String): DiagnosticArea?

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE id = :id")
    fun getPointById(id: String): Flow<DiagnosticPointWithHistory>

    @Query("SELECT * FROM diagnostic_points WHERE id = :id")
    fun getPointByIdOnce(id: String): DiagnosticPoint?

    @Transaction
    @Query("SELECT * FROM diagnostic_areas WHERE diagnostic_object = :diagnosticObject ORDER BY id ASC")
    fun getObjectAreas(diagnosticObject: String): Flow<List<DiagnosticAreaWithPoints>>

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE diagnostic_area = :diagnosticArea ORDER BY id ASC")
    fun getAreaPoints(diagnosticArea: String): Flow<List<DiagnosticPointWithHistory>>

    @Transaction
    @Query("SELECT * FROM diagnostic_reports WHERE diagnostic_point = :diagnosticPoint ORDER BY id ASC")
    fun getPointReports(diagnosticPoint: String): Flow<List<DiagnosticReportWithData>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDiagnosticObject(diagnosticObject: DiagnosticObject)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDiagnosticArea(diagnosticArea: DiagnosticArea)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDiagnosticPoint(diagnosticPoint: DiagnosticPoint)

    @Update
    fun updateDiagnosticObject(diagnosticObject: DiagnosticObject)

    @Update
    fun updateDiagnosticArea(area: DiagnosticArea)

    @Update
    fun updateDiagnosticPoint(point: DiagnosticPoint)

    @Delete
    fun deleteDiagnosticObject(diagnosticObject: DiagnosticObject)

    @Delete
    fun deleteDiagnosticArea(area: DiagnosticArea)

    @Delete
    fun deleteDiagnosticPoint(point: DiagnosticPoint)
}