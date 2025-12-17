package space.diomentia.ehzdiagnosticstracker.data

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json

val Json.Default.default: Json
    get() = Json { prettyPrint = true }

val Yaml.Companion.appConf: Yaml
    get() = Yaml(configuration = YamlConfiguration(decodeEnumCaseInsensitive = true))