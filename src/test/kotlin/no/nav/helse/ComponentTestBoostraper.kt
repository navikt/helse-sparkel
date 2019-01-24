package no.nav.helse

import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.client.*
import com.github.tomakehurst.wiremock.core.*
import io.prometheus.client.*
import no.nav.helse.ws.*

fun bootstrapComponentTest() : ComponentTestBootstrap {
    val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    wireMockServer.start()
    WireMock.configureFor(wireMockServer.port())

    WireMock.stubFor(stsRestStub())
    WireMock.stubFor(stsStub("stsUsername", "stsPassword")
            .willReturn(samlAssertionResponse("testusername", "theIssuer", "CN=B27 Issuing CA Intern, DC=preprod, DC=local",
                    "digestValue", "signatureValue", "certificateValue")))

    val jwkStub = JwtStub("test issuer")
    val env = Environment(mapOf(
            "JWT_ISSUER" to "test issuer",
            "AKTORREGISTER_URL" to wireMockServer.baseUrl(),
            "SECURITY_TOKEN_SERVICE_REST_URL" to wireMockServer.baseUrl(),
            "SECURITY_TOKEN_SERVICE_URL" to wireMockServer.baseUrl().plus("/sts"),
            "SECURITY_TOKEN_SERVICE_USERNAME" to "stsUsername",
            "SECURITY_TOKEN_SERVICE_PASSWORD" to "stsPassword",
            "AAREG_ENDPOINTURL" to wireMockServer.baseUrl().plus("/aareg"),
            "ALLOW_INSECURE_SOAP_REQUESTS" to "true"
    ))

    return ComponentTestBootstrap(
            env = env,
            jwkStub = jwkStub,
            wireMockServer = wireMockServer
    )
}

data class ComponentTestBootstrap(
        val env: Environment,
        val jwkStub: JwtStub,
        val wireMockServer: WireMockServer
) {

    fun start() {
    }

    fun stop() {
        wireMockServer.stop()
    }

    fun reset() {
        WireMock.reset()
        CollectorRegistry.defaultRegistry.clear()
    }
}
