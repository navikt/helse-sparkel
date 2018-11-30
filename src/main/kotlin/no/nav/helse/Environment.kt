package no.nav.helse

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class Environment(val map: Map<String, String> = System.getenv()) {

    val securityTokenServiceEndpointUrl: String by envVar("SECURITY_TOKEN_SERVICE_URL")
    val securityTokenUsername: String by envVar("SECURITY_TOKEN_SERVICE_USERNAME")
    val securityTokenPassword: String by envVar("SECURITY_TOKEN_SERVICE_PASSWORD")
    val personEndpointUrl: String by envVar("PERSON_ENDPOINTURL")
    val inntektEndpointUrl: String by envVar("INNTEKT_ENDPOINTURL")
    val arbeidsforholdEndpointUrl:String by envVar("AAREG_ENDPOINTURL")
    val organisasjonEndpointUrl: String by envVar("ORGANISASJON_ENDPOINTURL")
    val sakOgBehandlingEndpointUrl: String by envVar("SAK_OG_BEHANDLING_ENDPOINTURL")
    val jwksUrl: String by envVar("JWKS_URL")
    val jwtIssuer: String by envVar("JWT_ISSUER")

    private fun envVar(key: String): ReadOnlyProperty<Environment, String> {
        return object : ReadOnlyProperty<Environment, String> {
            override operator fun getValue(thisRef: Environment, property: KProperty<*>): String {
                return map[key] ?: throw RuntimeException("Missing required variable \"$key\"")
            }
        }
    }
}
