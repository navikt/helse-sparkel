package no.nav.helse

data class Environment(val map: Map<String, String> = System.getenv()) {

    val securityTokenServiceEndpointUrl: String = envVar("SECURITY_TOKEN_SERVICE_URL")
    val securityTokenUsername: String = envVar("SECURITY_TOKEN_SERVICE_USERNAME")
    val securityTokenPassword: String = envVar("SECURITY_TOKEN_SERVICE_PASSWORD")
    val personEndpointUrl: String = envVar("PERSON_ENDPOINTURL")
    val inntektEndpointUrl: String = envVar("INNTEKT_ENDPOINTURL")
    val arbeidsforholdEndpointUrl:String = envVar("AAREG_ENDPOINTURL")
    val organisasjonEndpointUrl: String = envVar("ORGANISASJON_ENDPOINTURL")
    val sakOgBehandlingEndpointUrl: String = envVar("SAK_OG_BEHANDLING_ENDPOINTURL")
    val hentSykePengeListeEndpointUrl: String = envVar("HENT_SYKEPENGER_ENDPOINTURL")
    val finnInfotrygdGrunnlagListeEndpointUrl: String = envVar("FINN_INFOTRYGD_GRUNNLAG_LISTE_ENDPOINTURL")
    val hentSykePengeperiodeEndpointUrl: String = envVar("HENT_SYKEPENGEPERIODER_ENDPOINTURL")
    val meldekortEndpointUrl: String = envVar("MELDEKORT_UTBETALINGSGRUNNLAG_ENDPOINTURL")
    val arbeidsfordelingEndpointUrl: String = envVar("ARBEIDSFORDELING_ENDPOINTURL")
    val jwksUrl: String = envVar("JWKS_URL")
    val jwtIssuer: String = envVar("JWT_ISSUER")
    val aktørregisterUrl: String = envVar("AKTORREGISTER_URL")
    val infotrygdSakEndpoint: String = envVar("INFOTRYGD_SAK_ENDPOINTURL")

    val stsRestUrl: String = envVar("SECURITY_TOKEN_SERVICE_REST_URL")

    private fun envVar(key: String, defaultValue: String? = null): String {
        return map[key] ?: defaultValue ?: throw RuntimeException("Missing required variable \"$key\"")
    }
}
