package no.nav.helse

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.mockk.mockk
import no.nav.helse.domene.aiy.ArbeidInntektYtelseService
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.arbeid.ArbeidsforholdService
import no.nav.helse.domene.arbeid.ArbeidsgiverService
import no.nav.helse.domene.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.domene.infotrygd.InfotrygdBeregningsgrunnlagService
import no.nav.helse.domene.organisasjon.OrganisasjonService
import no.nav.helse.domene.person.PersonService
import no.nav.helse.domene.sykepengegrunnlag.SykepengegrunnlagService
import no.nav.helse.domene.sykepengehistorikk.SykepengehistorikkService
import no.nav.helse.domene.ytelse.YtelseService

fun Application.mockedSparkel(jwtIssuer: String = "", jwkProvider: JwkProvider, arbeidsfordelingService: ArbeidsfordelingService = mockk(),
                              arbeidsforholdService: ArbeidsforholdService = mockk(),
                              arbeidsgiverService: ArbeidsgiverService = mockk(),
                              arbeidInntektYtelseService: ArbeidInntektYtelseService = mockk(),
                              organisasjonService: OrganisasjonService = mockk(),
                              personService: PersonService = mockk(),
                              sykepengegrunnlagService: SykepengegrunnlagService = mockk(),
                              infotrygdBeregningsgrunnlagService: InfotrygdBeregningsgrunnlagService = mockk(),
                              aktørregisterService: AktørregisterService = mockk(),
                              sykepengehistorikkService: SykepengehistorikkService = mockk(),
                              ytelseService: YtelseService = mockk()
                              ) {
    return sparkel(jwtIssuer, jwkProvider, arbeidsfordelingService, arbeidsforholdService, arbeidsgiverService,
            arbeidInntektYtelseService, organisasjonService,
            personService, sykepengegrunnlagService, infotrygdBeregningsgrunnlagService,
            aktørregisterService, sykepengehistorikkService, ytelseService)
}
