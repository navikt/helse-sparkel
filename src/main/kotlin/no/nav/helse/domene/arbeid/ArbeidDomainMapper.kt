package no.nav.helse.domene.arbeid

import no.nav.helse.common.toLocalDate
import no.nav.helse.domene.arbeid.domain.Arbeidsavtale
import no.nav.helse.domene.arbeid.domain.Arbeidsforhold
import no.nav.helse.domene.arbeid.domain.Permisjon
import no.nav.helse.domene.inntekt.domain.Virksomhet
import no.nav.helse.domene.organisasjon.domain.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Person
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.AktoerId
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ArbeidsforholdFrilanser
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent
import org.slf4j.LoggerFactory

object ArbeidDomainMapper {

    private val log = LoggerFactory.getLogger(ArbeidDomainMapper::class.java)

    fun toArbeidsforhold(arbeidsforhold: no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold) =
            when (arbeidsforhold.arbeidsgiver) {
                is Organisasjon -> Virksomhet.Organisasjon(Organisasjonsnummer((arbeidsforhold.arbeidsgiver as Organisasjon).orgnummer))
                is Person -> Virksomhet.Person((arbeidsforhold.arbeidsgiver as Person).ident.ident)
                else -> {
                    log.error("unknown arbeidsgivertype: ${arbeidsforhold.arbeidsgiver}")
                    null
                }
            }?.let { arbeidsgiver ->
                Arbeidsforhold.Arbeidstaker(
                        arbeidsgiver = arbeidsgiver,
                        startdato = arbeidsforhold.ansettelsesPeriode.periode.fom.toLocalDate(),
                        sluttdato = arbeidsforhold.ansettelsesPeriode.periode.tom?.toLocalDate(),
                        arbeidsforholdId = arbeidsforhold.arbeidsforholdIDnav,
                        arbeidsavtaler = arbeidsforhold.arbeidsavtale.map(::toArbeidsavtale),
                        permisjon = arbeidsforhold.permisjonOgPermittering.map { permisjonOgPermittering ->
                            Permisjon(
                                    fom = permisjonOgPermittering.permisjonsPeriode.fom.toLocalDate(),
                                    tom = permisjonOgPermittering.permisjonsPeriode.tom?.toLocalDate(),
                                    permisjonsprosent = permisjonOgPermittering.permisjonsprosent,
                                    årsak = permisjonOgPermittering.permisjonOgPermittering.value
                            )
                        }
                )
            }

    fun toArbeidsforhold(arbeidsforhold: ArbeidsforholdFrilanser) =
            when (arbeidsforhold.arbeidsgiver) {
                is PersonIdent -> Virksomhet.Person((arbeidsforhold.arbeidsgiver as PersonIdent).personIdent)
                is AktoerId -> Virksomhet.NavAktør((arbeidsforhold.arbeidsgiver as AktoerId).aktoerId)
                is no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon -> Virksomhet.Organisasjon(Organisasjonsnummer((arbeidsforhold.arbeidsgiver as no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Organisasjon).orgnummer))
                else -> null
            }?.let { virksomhet ->
                Arbeidsforhold.Frilans(
                        arbeidsgiver = virksomhet,
                        startdato = arbeidsforhold.frilansPeriode.fom.toLocalDate(),
                        sluttdato = arbeidsforhold.frilansPeriode?.tom?.toLocalDate(),
                        yrke = if (arbeidsforhold.yrke?.value.isNullOrBlank()) "UKJENT" else arbeidsforhold.yrke.value
                )
            }

    fun toArbeidsavtale(arbeidsavtale: no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsavtale) =
            Arbeidsavtale(
                    yrke = arbeidsavtale.yrke.value,
                    stillingsprosent = arbeidsavtale.stillingsprosent,
                    fom = arbeidsavtale.fomGyldighetsperiode.toLocalDate(),
                    tom = arbeidsavtale.tomGyldighetsperiode?.toLocalDate()
            )
}
