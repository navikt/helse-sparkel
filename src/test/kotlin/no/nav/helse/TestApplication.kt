package no.nav.helse

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.mockk.mockk
import no.nav.helse.domene.aiy.ArbeidInntektYtelseService
import no.nav.helse.domene.aiy.SykepengegrunnlagService
import no.nav.helse.domene.aiy.aareg.ArbeidsgiverService
import no.nav.helse.domene.aiy.aareg.ArbeidstakerService
import no.nav.helse.domene.aiy.organisasjon.OrganisasjonService
import no.nav.helse.domene.aktør.AktørregisterService
import no.nav.helse.domene.arbeidsfordeling.ArbeidsfordelingService
import no.nav.helse.domene.person.PersonService
import no.nav.helse.domene.ytelse.sykepengehistorikk.SykepengehistorikkService
import no.nav.helse.domene.ytelse.YtelseService

fun Application.mockedSparkel(jwtIssuer: String = "", jwkProvider: JwkProvider, arbeidsfordelingService: ArbeidsfordelingService = mockk(),
                              arbeidstakerService: ArbeidstakerService = mockk(),
                              arbeidsgiverService: ArbeidsgiverService = mockk(),
                              arbeidInntektYtelseService: ArbeidInntektYtelseService = mockk(),
                              organisasjonService: OrganisasjonService = mockk(),
                              personService: PersonService = mockk(),
                              sykepengegrunnlagService: SykepengegrunnlagService = mockk(),
                              aktørregisterService: AktørregisterService = mockk(),
                              sykepengehistorikkService: SykepengehistorikkService = mockk(),
                              ytelseService: YtelseService = mockk()
                              ) {
    return sparkel(jwtIssuer, jwkProvider, arbeidsfordelingService, arbeidstakerService, arbeidsgiverService,
            arbeidInntektYtelseService, organisasjonService,
            personService, sykepengegrunnlagService,
            aktørregisterService, sykepengehistorikkService, ytelseService)
}
