package space.diomentia.ehzdiagnosticstracker.data

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.reflect.KClass

@Serializable
enum class ParameterType(val kClass: KClass<*>) {
    Text(String::class),
    Bool(Boolean::class),
    Integer(Int::class),
    Decimal(Float::class),
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
    val parameters: List<ReportParameter>
)

@Serializable
data class ReportConfig(
    val reportTypes: List<ReportType>
) {
    companion object {
        fun parseYaml(config: String): ReportConfig = Yaml.default.decodeFromString(config)
    }
}