package no.nav.helse.domene.ytelse.domain

data class InfotrygdSakOgGrunnlag(
        val sak: InfotrygdSak,
        val grunnlag: Beregningsgrunnlag?
)
