package space.diomentia.ehzdiagnosticstracker.data

import android.location.Location
import androidx.room.TypeConverter
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json
import java.time.Instant

val Json.Default.default: Json
    get() = Json { prettyPrint = true }

val Yaml.Companion.appConf: Yaml
    get() = Yaml(configuration = YamlConfiguration(decodeEnumCaseInsensitive = true))

private val UUID_REGEX =
    Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

fun isCanonicalUuid(s: String): Boolean = UUID_REGEX.matches(s)

// Instant.toString() always emits canonical UTC ISO-8601 (...Z, fixed layout).
// Lex-ordering on the resulting TEXT column matches chronological order, so
// SQLite ORDER BY clauses on these columns stay correct as long as this
// converter remains the only write path.
class InstantConverter {
    @TypeConverter
    fun fromString(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun toString(value: Instant?): String? = value?.toString()
}

fun Coordinates.distanceKmTo(other: Coordinates): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        lat, lng,
        other.lat, other.lng,
        results
    )
    return results[0].toDouble() / 1000.0
}