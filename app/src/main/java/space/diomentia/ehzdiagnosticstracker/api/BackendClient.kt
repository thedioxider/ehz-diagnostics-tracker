package space.diomentia.ehzdiagnosticstracker.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import space.diomentia.ehzdiagnosticstracker.data.FullReport

const val COMPILED_CONTRACT_VERSION = 1

sealed interface IngestResult {
    data object Ok : IngestResult
    data object AuthFailed : IngestResult
    data class VersionMismatch(val expected: Int, val got: Int?) : IngestResult
    data class NetworkError(val cause: Throwable) : IngestResult
    data class HttpError(val code: Int, val body: String) : IngestResult
}

class BackendClient(private val baseUrl: String, private val bearer: String) {
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url(baseUrl)
            header(HttpHeaders.Authorization, "Bearer $bearer")
            contentType(ContentType.Application.Json)
        }
        expectSuccess = false
    }

    suspend fun ingest(report: FullReport): IngestResult = runCatching {
        val dto = report.toDto()
        val resp = http.put("/api/reports/${dto.id}") { setBody(dto) }
        checkContractHeader(resp)?.let { return it }
        when (resp.status.value) {
            200, 201 -> IngestResult.Ok
            401 -> IngestResult.AuthFailed
            else -> IngestResult.HttpError(resp.status.value, resp.bodyAsText())
        }
    }.getOrElse { IngestResult.NetworkError(it) }

    suspend fun ping(): IngestResult = runCatching {
        val resp = http.get("/api/ping")
        checkContractHeader(resp)?.let { return it }
        when (resp.status.value) {
            200 -> IngestResult.Ok
            401 -> IngestResult.AuthFailed
            else -> IngestResult.HttpError(resp.status.value, resp.bodyAsText())
        }
    }.getOrElse { IngestResult.NetworkError(it) }

    private fun checkContractHeader(resp: io.ktor.client.statement.HttpResponse): IngestResult? {
        val got = resp.headers["X-Contract-Version"]?.toIntOrNull()
        return if (got != COMPILED_CONTRACT_VERSION)
            IngestResult.VersionMismatch(COMPILED_CONTRACT_VERSION, got)
        else null
    }
}
