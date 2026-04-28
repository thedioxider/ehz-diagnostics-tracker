package space.diomentia.ehzdiagnosticstracker.api

import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.FullReport
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields

fun FullReport.toDto(): ReportDto = ReportDto(
    id = info.id,
    inspector = info.inspectorName,
    route = RouteDto(
        id = route.id,
        name = route.name,
        lengthKm = route.lengthKm
    ),
    section = SectionDto(
        id = section.id,
        beginKm = section.beginKm,
        endKm = section.endKm
    ),
    startTime = info.startTime.toString(),
    endTime = (info.endTime
        ?: error("Cannot map unfinished report ${info.id}: endTime is null")).toString(),
    sites = sites
        .filter { !it.info.isDraft }
        .map { it.toDto() }
)

private fun SiteWithFields.toDto(): SiteDto = SiteDto(
    id = info.id,
    timestamp = info.timestamp.toString(),
    positionKm = info.positionKm,
    location = info.location?.toDto(),
    validationId = info.validationId,
    fields = if (info.locationOnly) null else fields.map { f ->
        FieldDto(
            specId = f.specId,
            name = f.name,
            type = f.type,
            value = f.value,
            unit = f.unit
        )
    }
)

private fun Coordinates.toDto() = CoordinatesDto(lat = lat, lng = lng)
