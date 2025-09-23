package space.diomentia.ehzdiagnosticstracker.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction

const val REPORT_DATABASE_NAME = "report-db"

@Database(
    version = 1,
    entities = [
        DiagnosticReport::class,
        ReportDataEntry::class
    ]
)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun diagnosticReportDao(): DiagnosticReportDao
}

data class Coordinates(
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Double
)

@Entity(
    tableName = "diagnostic_reports",
    indices = [
        Index("area_id", "timestamp", unique = true, name = "report_id")
    ]
)
data class DiagnosticReport(
    @ColumnInfo(name = "is_complete") val isComplete: Boolean,
    @ColumnInfo(name = "area_id") val areaId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "diagnostics_type") val diagnosticsType: Int,
    @ColumnInfo(name = "report_type") val reportType: String,
    @Embedded val location: Coordinates,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)

@Entity(tableName = "data_entries", primaryKeys = ["report", "characteristic"])
data class ReportDataEntry(
    @ColumnInfo(name = "report") val report: Long,
    @ColumnInfo(name = "characteristic") val characteristic: String,
    @ColumnInfo(name = "value") val value: String
)

data class DiagnosticReportRelative(
    @Embedded val report: DiagnosticReport,
    @Relation(parentColumn = "id", entityColumn = "report") val data: List<ReportDataEntry>
)

@Dao
interface DiagnosticReportDao {
    @Transaction
    @Query("SELECT * FROM diagnostic_reports")
    fun getReports(): List<DiagnosticReportRelative>

    @Insert
    fun insertReport(report: DiagnosticReport): Long

    @Insert
    fun insertData(vararg data: ReportDataEntry)
}