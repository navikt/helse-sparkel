package no.nav.helse

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.mockk.mockk
import no.nav.helse.http.aktør.AktørregisterService
import no.nav.helse.ws.aiy.ArbeidInntektYtelseService
import no.nav.helse.ws.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdService
import no.nav.helse.ws.arbeidsforhold.ArbeidsgiverService
import no.nav.helse.ws.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagService
import no.nav.helse.ws.meldekort.MeldekortService
import no.nav.helse.ws.organisasjon.OrganisasjonService
import no.nav.helse.ws.person.PersonService
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingService
import no.nav.helse.ws.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.ws.sykepengehistorikk.SykepengehistorikkService
import no.nav.helse.ws.sykepenger.SykepengelisteService

fun Application.mockedSparkel(jwtIssuer: String = "", jwkProvider: JwkProvider, arbeidsfordelingService: ArbeidsfordelingService = mockk(),
                              arbeidsforholdService: ArbeidsforholdService = mockk(),
                              arbeidsgiverService: ArbeidsgiverService = mockk(),
                              arbeidInntektYtelseService: ArbeidInntektYtelseService = mockk(),
                              meldekortService: MeldekortService = mockk(),
                              organisasjonService: OrganisasjonService = mockk(),
                              personService: PersonService = mockk(),
                              sakOgBehandlingService: SakOgBehandlingService = mockk(),
                              sykepengegrunnlagService: SykepengegrunnlagService = mockk(),
                              sykepengelisteService: SykepengelisteService = mockk(),
                              infotrygdBeregningsgrunnlagService: InfotrygdBeregningsgrunnlagService = mockk(),
                              aktørregisterService: AktørregisterService = mockk(),
                              sykepengehistorikkService: SykepengehistorikkService = mockk()
                              ) {
    return sparkel(jwtIssuer, jwkProvider, arbeidsfordelingService, arbeidsforholdService, arbeidsgiverService,
            arbeidInntektYtelseService, meldekortService, organisasjonService,
            personService, sakOgBehandlingService, sykepengegrunnlagService, sykepengelisteService, infotrygdBeregningsgrunnlagService,
            aktørregisterService, sykepengehistorikkService)
}
