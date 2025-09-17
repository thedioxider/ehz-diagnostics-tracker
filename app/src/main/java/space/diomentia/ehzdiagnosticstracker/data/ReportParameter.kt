package space.diomentia.ehzdiagnosticstracker.data

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
enum class ParameterType {
    Text, Int, Float, Bool
}

@Serializable
data class ReportParameter(
    val name: String,
    val type: ParameterType,
    val unit: String? = null
)

@Serializable
data class ReportClass(
    val name: String,
    val parameters: List<ReportParameter>
)

@Serializable
data class ReportConfig(
    val classes: List<ReportClass>
) {
    companion object {
        fun parseYaml(config: String): ReportConfig = Yaml.default.decodeFromString(config)
    }
}