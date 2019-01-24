package no.nav.helse

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class Environment(val map: Map<String, String> = System.getenv()) {

    val securityTokenServiceEndpointUrl: String by lazyEnvVar("SECURITY_TOKEN_SERVICE_URL")
    val securityTokenUsername: String by lazyEnvVar("SECURITY_TOKEN_SERVICE_USERNAME")
    val securityTokenPassword: String by lazyEnvVar("SECURITY_TOKEN_SERVICE_PASSWORD")
    val personEndpointUrl: String by lazyEnvVar("PERSON_ENDPOINTURL")
    val inntektEndpointUrl: String by lazyEnvVar("INNTEKT_ENDPOINTURL")
    val arbeidsforholdEndpointUrl:String by lazyEnvVar("AAREG_ENDPOINTURL")
    val organisasjonEndpointUrl: String by lazyEnvVar("ORGANISASJON_ENDPOINTURL")
    val sakOgBehandlingEndpointUrl: String by lazyEnvVar("SAK_OG_BEHANDLING_ENDPOINTURL")
    val hentSykePengeListeEndpointUrl: String by lazyEnvVar("HENT_SYKEPENGER_ENDPOINTURL")
    val meldekortEndpointUrl: String by lazyEnvVar("MELDEKORT_UTBETALINGSGRUNNLAG_ENDPOINTURL")
    val jwksUrl: String by lazyEnvVar("JWKS_URL")
    val jwtIssuer: String by lazyEnvVar("JWT_ISSUER")
    val aktørregisterUrl: String by lazyEnvVar("AKTORREGISTER_URL")

    val stsRestUrl: String by lazyEnvVar("SECURITY_TOKEN_SERVICE_REST_URL")
    val allowInsecureSoapRequests: Boolean by lazyEnvVar("ALLOW_INSECURE_SOAP_REQUESTS", "false") { value -> "true" == value }

    private fun lazyEnvVar(key: String): ReadOnlyProperty<Environment, String> {
        return lazyEnvVar(key, null) { value -> value }
    }
    private fun <R> lazyEnvVar(key: String, defaultValue: String? = null, mapper: ((String) -> R)): ReadOnlyProperty<Environment, R> {
        return object : ReadOnlyProperty<Environment, R> {
            override operator fun getValue(thisRef: Environment, property: KProperty<*>) = mapper(envVar(key, defaultValue))
        }
    }

    private fun envVar(key: String, defaultValue: String? = null): String {
        return map[key] ?: defaultValue ?: throw RuntimeException("Missing required variable \"$key\"")
    }
}
