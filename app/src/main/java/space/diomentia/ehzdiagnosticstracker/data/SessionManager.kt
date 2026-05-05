package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.util.UUID

class SessionManager(private val dao: ReportDao, private val config: ReportConfig?) {
    var activeReportId: String? = null
        private set

    var profile: Profile? = null

    fun getSite(siteId: String): Site? = dao.getSiteByIdOnce(siteId)
    fun getFieldsForSite(siteId: String): List<ReportField> = dao.getFieldsForSiteOnce(siteId)

    fun hasNoSites(): Boolean {
        val reportId = activeReportId ?: return true
        val report = dao.getReportByIdOnce(reportId) ?: return true
        return dao.getActiveReportForSectionOnce(report.sectionId)
            ?.sites.isNullOrEmpty()
    }

    val fullReport: Flow<FullReport?>
        get() = activeReportId?.let { dao.getFullReport(it) } ?: flowOf(null)

    fun startOrResumeSession(routeId: String, sectionId: String): Boolean {
        val active = dao.getActiveReportForSectionOnce(sectionId)
        if (active != null) {
            activeReportId = active.info.id
            return false
        }
        val id = UUID.randomUUID().toString()
        dao.insertReport(
            Report(
                id = id,
                routeId = routeId,
                sectionId = sectionId,
                inspectorName = profile?.fullName ?: "",
                startTime = Instant.now()
            )
        )
        activeReportId = id
        return true
    }

    fun leaveSession() {
        activeReportId = null
    }

    fun deleteSession() {
        activeReportId?.let {
            dao.deleteReportById(it)
        }
        activeReportId = null
    }

    fun addSite(
        location: Coordinates? = null,
        positionKm: Double? = null,
        locationOnly: Boolean = false
    ): Site {
        val reportId = activeReportId ?: error("No active report")
        // TODO: configuration handling throughout the app
        if (config == null) error("No configuration")
        val siteId = UUID.randomUUID().toString()
        val site = Site(
            id = siteId,
            reportId = reportId,
            positionKm = positionKm,
            location = location,
            timestamp = Instant.now(),
            locationOnly = locationOnly
        )
        dao.insertSite(site)
        config.defaultSpecs.forEach { specId ->
            dao.insertFieldFromConfig(
                siteId = siteId,
                specId = specId,
                config = config
            )
        }
        return site
    }

    fun updateSiteName(siteId: String, name: String) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(site.copy(name = name.trim()))
    }

    fun updateSitePosition(siteId: String, positionKm: Double) {
        val sites = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(sites.copy(positionKm = positionKm))
    }

    fun updateSiteLocation(siteId: String, location: Coordinates) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(site.copy(location = location))
    }

    fun setSiteValidation(siteId: String, validationId: String?) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(site.copy(validationId = validationId))
    }

    fun addFieldToSite(siteId: String, spec: FieldSpec) {
        dao.insertField(
            ReportField(
                siteId = siteId,
                specId = spec.id,
                name = spec.name,
                type = spec.type.datatype,
                unit = spec.unit
            )
        )
    }

    fun applyTemplateToSite(siteId: String, template: ReportTemplate) {
        val specs = config?.resolveTemplate(template) ?: return
        val fields = specs.map { spec ->
            ReportField(
                siteId = siteId,
                specId = spec.id,
                name = spec.name,
                type = spec.type.datatype,
                unit = spec.unit
            )
        }
        dao.insertFields(fields)
    }

    fun updateFieldValue(siteId: String, specId: String, value: String) {
        val fields = dao.getFieldsForSiteOnce(siteId)
        val field = fields.find { it.specId == specId } ?: return
        val label = config?.getSpec(specId)?.enumValue(value)?.label
        val normalizedValue = when (FieldType.parse(field.type)) {
            FieldType.Decimal -> value.replace(',', '.').toBigDecimalOrNull()?.toPlainString() ?: ""
            FieldType.Integer -> value.toLongOrNull()?.toString() ?: ""
            else -> value.trim()
        }
        dao.insertField(field.copy(value = normalizedValue, valueLabel = label))
    }

    fun confirmSite(siteId: String) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(site.copy(isDraft = false))
    }

    fun getDraftSite(): SiteWithFields? {
        val reportId = activeReportId ?: return null
        return dao.getDraftSiteForReport(reportId)
    }

    fun removeSite(siteId: String) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.deleteSite(site)
    }

    fun removeFieldFromSite(siteId: String, specId: String) {
        dao.deleteFieldByKey(siteId, specId)
    }

    fun cancelReport() {
        val reportId = activeReportId ?: return
        val report = dao.getReportByIdOnce(reportId) ?: return
        dao.deleteReport(report)
        activeReportId = null
    }

    fun completeReport() {
        val reportId = activeReportId ?: return
        val report = dao.getReportByIdOnce(reportId) ?: return
        dao.deleteDraftSiteForReport(reportId)
        dao.updateReport(
            report.copy(
                endTime = Instant.now(),
                isComplete = true
            )
        )
        activeReportId = null
    }
}
