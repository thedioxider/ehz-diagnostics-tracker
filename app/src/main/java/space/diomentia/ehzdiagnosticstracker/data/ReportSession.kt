package space.diomentia.ehzdiagnosticstracker.data

data class ReportSession(
    val route: Route,
    val section: Section,
    val diagnosticType: Int = 0
)