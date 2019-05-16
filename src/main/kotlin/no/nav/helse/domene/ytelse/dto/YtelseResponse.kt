package no.nav.helse.domene.ytelse.dto

import no.nav.helse.domene.ytelse.domain.Ytelse

data class YtelseResponse(val ytelser: List<Ytelse>)
