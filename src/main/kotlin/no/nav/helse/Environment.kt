package no.nav.helse

data class Environment(val securityTokenServiceEndpointUrl: String = getEnvVar("SECURITY_TOKEN_SERVICE_URL"),
                       val securityTokenUsername: String = getEnvVar("SECURITY_TOKEN_SERVICE_USERNAME"),
                       val securityTokenPassword: String = getEnvVar("SECURITY_TOKEN_SERVICE_PASSWORD"),
                       val personEndpointUrl: String = getEnvVar("PERSON_ENDPOINTURL"),
                       val inntektEndpointUrl: String = getEnvVar("INNTEKT_ENDPOINTURL"),
                       val arbeidsforholdEndpointUrl: String = getEnvVar("AAREG_ENDPOINTURL"),
                       val organisasjonEndpointUrl: String = getEnvVar("ORGANISASJON_ENDPOINTURL"),
                       val sakOgBehandlingEndpointUrl: String = getEnvVar("SAK_OG_BEHANDLING_ENDPOINTURL"),
                       val jwksUrl: String = getEnvVar("JWKS_URL"),
                       val jwtIssuer: String = getEnvVar("JWT_ISSUER"),
                       val jwtAudience: String = getEnvVar("JWT_AUDIENCE"))

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")