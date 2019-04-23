package no.nav.helse.domene.arbeid.dto

import no.nav.helse.domene.organisasjon.dto.OrganisasjonDTO

data class ArbeidsgivereResponse(val arbeidsgivere: List<OrganisasjonDTO>)
