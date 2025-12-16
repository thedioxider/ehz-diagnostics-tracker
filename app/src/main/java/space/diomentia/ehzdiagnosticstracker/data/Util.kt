package space.diomentia.ehzdiagnosticstracker.data

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

val Yaml.Companion.appConf: Yaml
    get() = Yaml(configuration = YamlConfiguration(decodeEnumCaseInsensitive = true))