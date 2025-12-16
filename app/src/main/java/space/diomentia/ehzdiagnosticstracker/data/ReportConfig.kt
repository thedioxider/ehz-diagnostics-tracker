package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.serialization.Serializable

sealed class ReportFieldValue {
    data class Text(val value: String?) : ReportFieldValue()
    data class Bool(val value: Boolean?) : ReportFieldValue() {
        companion object {
            fun fromString(raw: String?) = when (raw) {
                "0", "false" -> false
                "1", "true" -> true
                else -> null
            }
        }
    }

    data class Integer(val value: Int?) : ReportFieldValue()
    data class Decimal(val value: Double?) : ReportFieldValue()
    data class Photo(val value: Any?) : ReportFieldValue()
    data class Raw(val value: String?) : ReportFieldValue()
}

@Serializable
enum class ParameterType {
    Text,
    Bool,
    Integer,
    Decimal,
    Photo,
    Raw;

    val datatype
        get() = this.name.lowercase()

    companion object {
        fun parse(s: String): ParameterType? =
            entries.find { it.name.equals(s, ignoreCase = true) }
    }
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