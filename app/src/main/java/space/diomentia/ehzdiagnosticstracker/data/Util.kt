package space.diomentia.ehzdiagnosticstracker.data

import android.location.Location
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json

val Json.Default.default: Json
    get() = Json { prettyPrint = true }

val Yaml.Companion.appConf: Yaml
    get() = Yaml(configuration = YamlConfiguration(decodeEnumCaseInsensitive = true))

fun Coordinates.distanceKmTo(other: Coordinates): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        lat, lng,
        other.lat, other.lng,
        results
    )
    return results[0].toDouble() / 1000.0
}