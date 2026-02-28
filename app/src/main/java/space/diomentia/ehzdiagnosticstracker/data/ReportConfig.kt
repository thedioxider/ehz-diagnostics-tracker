package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.serialization.SerialName
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
enum class FieldType {
    @SerialName("text")
    Text,
    @SerialName("bool")
    Bool,
    @SerialName("integer")
    Integer,
    @SerialName("decimal")
    Decimal,
    @SerialName("photo")
    Photo,
    @SerialName("raw")
    Raw;

    val datatype
        get() = this.name.lowercase()

    companion object {
        fun parse(s: String): FieldType? =
            entries.find { it.name.equals(s, ignoreCase = true) }
    }
}

@Serializable
data class FieldSpec(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: FieldType,
    @SerialName("unit") val unit: String? = null
)

@Serializable
data class ReportTemplate(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("parameters") val specIds: List<FieldSpec>?
)

@Serializable
data class ReportConfig(
    @SerialName("report_types") val templates: List<ReportTemplate>
)

@Serializable
data class InitialAreaEntries(
    @SerialName("diagnostic_objects") val routes: List<Route>,
    @SerialName("diagnostic_areas") val sections: List<Section>,
    @SerialName("diagnostic_points") val diagnosticPoints: List<DiagnosticPoint>
)