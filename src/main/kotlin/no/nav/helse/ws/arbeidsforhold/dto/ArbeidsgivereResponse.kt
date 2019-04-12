package no.nav.helse.ws.arbeidsforhold.dto

import no.nav.helse.ws.organisasjon.dto.OrganisasjonDTO

data class ArbeidsgivereResponse(val arbeidsgivere: List<OrganisasjonDTO>)
