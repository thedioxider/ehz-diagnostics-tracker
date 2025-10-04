package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
enum class ParameterType(val kClass: KClass<*>) {
    Text(String::class),
    Bool(Boolean::class),
    Integer(Int::class),
    Decimal(Float::class),
    Photo(Any::class),
}

@Serializable
data class ReportParameter(
    val id: String,
    val name: String,
    val type: ParameterType,
    val unit: String? = null
)

@Serializable
data class ReportType(
    val id: String,
    val name: String,
    val parameters: List<ReportParameter>?
)

@Serializable
data class ReportConfig(
    val reportTypes: List<ReportType>
)

@Serializable
data class InitialAreaEntries(
    val diagnosticObjects: List<DiagnosticObject>,
    val diagnosticAreas: List<DiagnosticArea>,
    val diagnosticPoints: List<DiagnosticPoint>
)