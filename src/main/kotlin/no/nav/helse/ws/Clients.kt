package no.nav.helse.ws

import no.nav.helse.Environment
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sts.STSClientBuilder
import no.nav.helse.ws.sts.STSProperties
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import org.apache.cxf.ws.security.trust.STSClient
import java.net.URI

class Clients(env: Environment) {
    private val stsClient: STSClient = STSClientBuilder().build(STSProperties(
            URI(env.securityTokenServiceEndpointUrl),
            env.securityTokenUsername,
            env.securityTokenPassword
    ))
    private val endpointSTSClientConfig = EndpointSTSClientConfig(stsClient)
    private val wsClientBuilder = WsClientBuilder(endpointSTSClientConfig)

    val personClient: PersonClient = wsClientBuilder.createSOAPClient(
            env.personEndpointUrl,
            PersonV3::class.java,
            PersonClient::class.java)

    val inntektClient: InntektClient = wsClientBuilder.createSOAPClient(
            env.inntektEndpointUrl,
            InntektV3::class.java,
            InntektClient::class.java)

    val arbeidsforholdClient: ArbeidsforholdClient = wsClientBuilder.createSOAPClient(
            env.arbeidsforholdEndpointUrl,
            ArbeidsforholdV3::class.java,
            ArbeidsforholdClient::class.java)

    val organisasjonClient: OrganisasjonClient = wsClientBuilder.createSOAPClient(
            env.organisasjonEndpointUrl,
            OrganisasjonV5::class.java,
            OrganisasjonClient::class.java)

    val sakOgBehandlingClient: SakOgBehandlingClient = wsClientBuilder.createSOAPClient(
            env.sakOgBehandlingEndpointUrl,
            SakOgBehandlingV1::class.java,
            SakOgBehandlingClient::class.java)
}
