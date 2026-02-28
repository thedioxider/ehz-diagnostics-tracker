package space.diomentia.ehzdiagnosticstracker.data

data class DiagnosticSession(
    val route: Route,
    val section: Section,
    val diagnosticType: Int = 0
)