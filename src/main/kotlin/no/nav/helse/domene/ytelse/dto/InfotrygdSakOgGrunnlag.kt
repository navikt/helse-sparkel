package no.nav.helse.domene.ytelse.dto

data class InfotrygdSakOgGrunnlagDto(
        val sak: InfotrygdSakDto,
        val grunnlag: List<BeregningsgrunnlagDto>
)
