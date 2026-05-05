package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    data class Enum(val code: String?) : ReportFieldValue()
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
    Raw,
    @SerialName("enum")
    Enum;

    val datatype
        get() = this.name.lowercase()

    companion object {
        fun parse(s: String): FieldType? =
            entries.find { it.name.equals(s, ignoreCase = true) }
    }
}

@Serializable
data class EnumValue(
    @SerialName("code") val code: String,
    @SerialName("label") val label: String
)

@Serializable
data class FieldSpec(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: FieldType,
    @SerialName("unit") val unit: String? = null,
    @SerialName("values") val values: List<EnumValue>? = null
) {
    @Transient
    private val valuesByCode: Map<String, EnumValue> =
        values?.associateBy { it.code } ?: emptyMap()

    /** Look up an allowed value by its code. Returns null for non-enum specs or unknown codes. */
    fun enumValue(code: String?): EnumValue? =
        if (type == FieldType.Enum && code != null) valuesByCode[code] else null
}

@Serializable
data class ReportTemplate(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("spec_ids") val specIds: List<String>
)

@Serializable
data class ReportConfig(
    @SerialName("field_specs") val fieldSpecs: List<FieldSpec>,
    @SerialName("default_specs") val defaultSpecs: List<String> = emptyList(),
    @SerialName("templates") val templates: List<ReportTemplate> = emptyList()
) {
    @Transient
    private val specsById: Map<String, FieldSpec> = fieldSpecs.associateBy { it.id }

    init {
        require(specsById.size == fieldSpecs.size) {
            "Duplicate field spec IDs in config"
        }
        for (spec in fieldSpecs) {
            if (spec.type == FieldType.Enum) {
                val values = spec.values
                require(!values.isNullOrEmpty()) {
                    "Field spec '${spec.id}' has type=enum but no values"
                }
                val codes = values.map { it.code }
                require(codes.toSet().size == codes.size) {
                    "Field spec '${spec.id}' has duplicate enum codes"
                }
            } else {
                require(spec.values == null) {
                    "Field spec '${spec.id}' has values but type=${spec.type.datatype}"
                }
            }
        }
    }

    /** Lookup a field spec by its ID */
    fun getSpec(id: String): FieldSpec? = specsById[id]

    /** Resolve a template's spec IDs to full FieldSpec objects */
    fun resolveTemplate(template: ReportTemplate): List<FieldSpec> =
        template.specIds?.mapNotNull { getSpec(it) } ?: emptyList()
}
