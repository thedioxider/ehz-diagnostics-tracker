package space.diomentia.ehzdiagnosticstracker.data

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationInfo(
    val organizationName: String,
    val head: String,
    val legalAddress: String,
    val location: String,
    val phone: String,
    val email: String
)