package no.nav.helse.domene.sykepengegrunnlag

import arrow.core.Either
import arrow.core.flatMap
import no.nav.helse.Feilårsak
import no.nav.helse.domene.AktørId
import no.nav.helse.domene.utbetaling.UtbetalingOgTrekkService
import no.nav.helse.domene.utbetaling.domain.Virksomhet
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.domene.organisasjon.domain.Organisasjon
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.time.YearMonth

class SykepengegrunnlagService(private val utbetalingOgTrekkService: UtbetalingOgTrekkService, private val organisasjonService: OrganisasjonService) {

    companion object {
        private val log = LoggerFactory.getLogger(SykepengegrunnlagService::class.java)

        private const val Sammenligningsgrunnlagfilter = "8-30"
        private const val Beregningsgrunnlagfilter = "8-28"
    }

    fun hentBeregningsgrunnlag(aktørId: AktørId, virksomhetsnummer: Organisasjonsnummer, fom: YearMonth, tom: YearMonth) =
            organisasjonService.hentOrganisasjon(virksomhetsnummer).flatMap { organisasjon ->
                when (organisasjon) {
                    is Organisasjon.Virksomhet -> {
                        utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, fom, tom, Beregningsgrunnlagfilter).map { inntekter ->
                            inntekter.filter { inntekt ->
                                inntekt.virksomhet is Virksomhet.Organisasjon
                            }
                        }.map { inntekter ->
                            inntekter.filter { inntekt ->
                                (inntekt.virksomhet as Virksomhet.Organisasjon).organisasjonsnummer == virksomhetsnummer
                                        || organisasjon.inngårIJuridiskEnhet.any { juridiskEnhet ->
                                    juridiskEnhet.organisasjonsnummer == (inntekt.virksomhet as Virksomhet.Organisasjon).organisasjonsnummer
                                }
                            }
                        }
                    }
                    else -> {
                        log.info("organisasjonsnummeret er ikke en virksomhet")
                        Either.Left(Feilårsak.FeilFraBruker)
                    }
                }
            }

    fun hentSammenligningsgrunnlag(aktørId: AktørId, fom: YearMonth, tom: YearMonth) =
            utbetalingOgTrekkService.hentUtbetalingerOgTrekk(aktørId, fom, tom, Sammenligningsgrunnlagfilter)
}
