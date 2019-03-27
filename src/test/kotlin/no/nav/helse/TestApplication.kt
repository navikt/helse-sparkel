package no.nav.helse

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.mockk.mockk
import no.nav.helse.ws.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.ws.aiy.ArbeidInntektYtelseService
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.inntekt.InntektService
import no.nav.helse.ws.meldekort.MeldekortService
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.person.PersonService
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingService
import no.nav.helse.ws.sykepenger.SykepengelisteService

fun Application.mockedSparkel(jwtIssuer: String = "", jwkProvider: JwkProvider, arbeidsfordelingService: ArbeidsfordelingService = mockk(),
                              arbeidsforholdService: ArbeidsforholdService = mockk(),
                              inntektService: InntektService = mockk(),
                              arbeidInntektYtelseService: ArbeidInntektYtelseService = mockk(),
                              meldekortService: MeldekortService = mockk(),
                              organisasjonService: OrganisasjonService = mockk(),
                              personService: PersonService = mockk(),
                              sakOgBehandlingService: SakOgBehandlingService = mockk(),
                              sykepengelisteService: SykepengelisteService = mockk()) {
    return sparkel(jwtIssuer, jwkProvider, arbeidsfordelingService, arbeidsforholdService, inntektService,
            arbeidInntektYtelseService, meldekortService, organisasjonService,
            personService, sakOgBehandlingService, sykepengelisteService)
}
