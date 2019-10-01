package no.nav.helse

import no.nav.helse.domene.ytelse.SpoleService
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException

data class Environment(val map: Map<String, String> = System.getenv()) {

    companion object  {
        private val log = LoggerFactory.getLogger(Environment::class.java)
    }

    val securityTokenServiceEndpointUrl: String = envVar("SECURITY_TOKEN_SERVICE_URL")
    val securityTokenUsername: String = envVar("SECURITY_TOKEN_SERVICE_USERNAME")
    val securityTokenPassword: String = envVar("SECURITY_TOKEN_SERVICE_PASSWORD")
    val personEndpointUrl: String = envVar("PERSON_ENDPOINTURL")
    val inntektEndpointUrl: String = envVar("INNTEKT_ENDPOINTURL")
    val arbeidsforholdEndpointUrl:String = envVar("AAREG_ENDPOINTURL")
    val organisasjonEndpointUrl: String = envVar("ORGANISASJON_ENDPOINTURL")
    val finnInfotrygdGrunnlagListeEndpointUrl: String = envVar("FINN_INFOTRYGD_GRUNNLAG_LISTE_ENDPOINTURL")
    val meldekortEndpointUrl: String = envVar("MELDEKORT_UTBETALINGSGRUNNLAG_ENDPOINTURL")
    val arbeidsfordelingEndpointUrl: String = envVar("ARBEIDSFORDELING_ENDPOINTURL")
    val jwksUrl: String = envVar("JWKS_URL")
    val jwtIssuer: String = envVar("JWT_ISSUER")
    val aktørregisterUrl: String = envVar("AKTORREGISTER_URL")
    val infotrygdSakEndpoint: String = envVar("INFOTRYGD_SAK_ENDPOINTURL")

    val stsRestUrl: String = envVar("SECURITY_TOKEN_SERVICE_REST_URL")

    val spoleUrl = envVar("SPOLE_URL", "http://spole.default.svc.nais.local")
    val azureTenantId = envVar("AZURE_TENANT_ID")
    val azureClientId = "/var/run/secrets/nais.io/azure/client_id".readFile() ?: envVar("AZURE_CLIENT_ID")
    val azureClientSecret = "/var/run/secrets/nais.io/azure/client_secret".readFile() ?: envVar("AZURE_CLIENT_SECRET")
    val spoleScope = envVar("SPOLE_SCOPE")

    private fun envVar(key: String, defaultValue: String? = null): String {
        return map[key] ?: defaultValue ?: throw RuntimeException("Missing required variable \"$key\"")
    }

    private fun String.readFile() =
            try {
                log.info("trying to read secret from $this")
                File(this).readText(Charsets.UTF_8)
            } catch (err: FileNotFoundException) {
                log.info("file not found", err)
                null
            }
}
