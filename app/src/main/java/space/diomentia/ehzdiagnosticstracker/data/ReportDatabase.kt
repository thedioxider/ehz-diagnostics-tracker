package space.diomentia.ehzdiagnosticstracker.data

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
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val REPORT_DATABASE_NAME = "report-db"

@Database(
    version = 3,
    entities = [
        DiagnosticReport::class,
        ReportDataEntry::class,
        DiagnosticObject::class,
        DiagnosticArea::class,
        DiagnosticPoint::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun diagnosticReportDao(): DiagnosticReportDao
    abstract fun diagnosticAreaDao(): DiagnosticAreaDao
}

data class Coordinates(
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Double
)

@Entity(
    tableName = "diagnostic_reports",
    indices = [
        Index("diagnostic_point", "timestamp", unique = true, name = "report_id")
    ]
)
data class DiagnosticReport(
    @ColumnInfo(name = "diagnostic_point") val diagnosticPoint: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "diagnostics_type") val diagnosticsType: String,
    @ColumnInfo(name = "report_type") val reportType: String,
    @Embedded val location: Coordinates,
    @ColumnInfo(name = "inspector") val inspector: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0
)

@Entity(tableName = "data_entries", primaryKeys = ["report", "characteristic"])
data class ReportDataEntry(
    @ColumnInfo(name = "report") val report: Long,
    @ColumnInfo(name = "characteristic") val characteristic: String,
    @ColumnInfo(name = "value") val value: String
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
    @Query("SELECT * FROM diagnostic_reports WHERE diagnostic_point = :pointId ORDER BY timestamp DESC")
    fun getPointReports(pointId: String): Flow<List<DiagnosticReportWithData>>

    @Query("SELECT diagnostic_point FROM diagnostic_reports")
    fun getPoints(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReport(report: DiagnosticReport): Long

    @Insert
    fun insertData(vararg data: ReportDataEntry)

    @Delete
    fun deleteReport(report: DiagnosticReport)

    @Delete
    fun deleteData(vararg data: ReportDataEntry)
}

@Serializable
@Entity(tableName = "diagnostic_objects")
data class DiagnosticObject(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "inventory_id") val inventoryId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "additional_info") val additionalInfo: String? = null
)

@Serializable
@Entity(tableName = "diagnostic_areas")
data class DiagnosticArea(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "diagnostic_object") val diagnosticObject: String,
    @ColumnInfo(name = "danger_class") val dangerClass: String,
    @ColumnInfo(name = "additional_info") val additionalInfo: String? = null
)

@Serializable
@Entity(tableName = "diagnostic_points")
data class DiagnosticPoint(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "diagnostic_area") val diagnosticArea: String,
    @ColumnInfo(name = "inventory_id") val inventoryId: String,
    @ColumnInfo(name = "registration_number") val registrationNumber: String,
    @ColumnInfo(name = "additional_info") val additionalInfo: String? = null
)

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
    @Query("SELECT * FROM diagnostic_objects")
    fun getAllObjects(): Flow<List<DiagnosticObjectWithAreas>>

    @Transaction
    @Query("SELECT * FROM diagnostic_areas WHERE id = :id")
    fun getAreaById(id: String): DiagnosticAreaWithPoints

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE id = :id")
    fun getPointById(id: String): DiagnosticPointWithHistory

    @Transaction
    @Query("SELECT * FROM diagnostic_areas WHERE diagnostic_object = :diagnosticObject")
    fun getObjectAreas(diagnosticObject: String): Flow<List<DiagnosticAreaWithPoints>>

    @Transaction
    @Query("SELECT * FROM diagnostic_points WHERE diagnostic_area = :diagnosticArea")
    fun getAreaPoints(diagnosticArea: String): Flow<List<DiagnosticPointWithHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDiagnosticObject(diagnosticObject: DiagnosticObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDiagnosticArea(diagnosticArea: DiagnosticArea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDiagnosticPoint(diagnosticPoint: DiagnosticPoint)
}