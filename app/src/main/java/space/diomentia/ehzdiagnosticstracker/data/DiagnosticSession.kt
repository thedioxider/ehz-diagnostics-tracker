package space.diomentia.ehzdiagnosticstracker.data

data class DiagnosticSession(
    val diagnosticObject: DiagnosticObject,
    val diagnosticArea: DiagnosticArea,
    val diagnosticType: Int = 0
)