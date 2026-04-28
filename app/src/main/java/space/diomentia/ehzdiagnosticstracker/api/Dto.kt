package space.diomentia.ehzdiagnosticstracker.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(
    val id: String,
    val inspector: String,
    val route: RouteDto,
    val section: SectionDto,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val sites: List<SiteDto>
)

@Serializable
data class RouteDto(
    val id: String,
    val name: String,
    @SerialName("length_km") val lengthKm: Int
)

@Serializable
data class SectionDto(
    val id: String,
    @SerialName("begin_km") val beginKm: Int,
    @SerialName("end_km") val endKm: Int
)

@Serializable
data class SiteDto(
    val id: String,
    val timestamp: String,
    @SerialName("position_km") val positionKm: Double? = null,
    val location: CoordinatesDto? = null,
    @SerialName("validation_id") val validationId: String? = null,
    val fields: List<FieldDto>? = null
)

@Serializable
data class CoordinatesDto(
    val lat: Double,
    val lng: Double
)

@Serializable
data class FieldDto(
    @SerialName("spec_id") val specId: String,
    val name: String,
    val type: String,
    val value: String,
    val unit: String? = null
)
