package no.nav.helse.domene.ytelse.dto

data class YtelseResponse(val arena: List<YtelseDto>, val infotrygd: List<InfotrygdSakOgGrunnlagDto>)
