package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    fun startOrResumeSession(route: Route, section: Section): Boolean {
        val active = dao.getActiveReportForSectionOnce(section.id)
        if (active != null) {
            activeReportId = active.report.id
            return false
        }
        val id = UUID.randomUUID().toString()
        dao.insertReport(
            Report(
                id = id,
                routeId = route.id,
                sectionId = section.id,
                inspectorName = profile?.fullName ?: "",
                startTime = System.currentTimeMillis() / 1000
            )
        )
        activeReportId = id
        return true
    }

    fun leaveSession() {
        activeReportId = null
    }

    fun addSite(
        latitude: Double? = null,
        longitude: Double? = null,
        positionKm: Double? = null,
        locationOnly: Boolean = false
    ): Site {
        val reportId = activeReportId ?: error("No active report")
        val site = Site(
            id = UUID.randomUUID().toString(),
            reportId = reportId,
            positionKm = positionKm,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis() / 1000,
            locationOnly = locationOnly
        )
        dao.insertSite(site)
        return site
    }

    fun updateSitePosition(siteId: String, positionKm: Double) {
        val sites = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(sites.copy(positionKm = positionKm))
    }

    fun updateSiteCoordinates(siteId: String, latitude: Double, longitude: Double) {
        val site = dao.getSiteByIdOnce(siteId) ?: return
        dao.updateSite(site.copy(latitude = latitude, longitude = longitude))
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
        dao.insertField(field.copy(value = value))
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
        dao.updateReport(
            report.copy(
                endTime = System.currentTimeMillis() / 1000,
                isComplete = true
            )
        )
        activeReportId = null
    }
}
