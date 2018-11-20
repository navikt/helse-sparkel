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
import java.net.URI

class Clients(env: Environment) {
    private val wsClientBuilder = WsClientBuilder()
    private val stsClient = STSClientBuilder().build(STSProperties(
            URI(env.securityTokenServiceEndpointUrl),
            env.securityTokenUsername,
            env.securityTokenPassword
    ))
    private val endpointSTSClientConfig = EndpointSTSClientConfig(stsClient)

    val personClient: PersonClient = wsClientBuilder.createSOAPClient(
            endpointSTSClientConfig,
            env.personEndpointUrl,
            PersonV3::class.java,
            PersonClient::class.java)

    val inntektClient: InntektClient = wsClientBuilder.createSOAPClient(
            endpointSTSClientConfig,
            env.inntektEndpointUrl,
            InntektV3::class.java,
            InntektClient::class.java)

    val arbeidsforholdClient: ArbeidsforholdClient = wsClientBuilder.createSOAPClient(
            endpointSTSClientConfig,
            env.arbeidsforholdEndpointUrl,
            ArbeidsforholdV3::class.java,
            ArbeidsforholdClient::class.java)

    val organisasjonClient: OrganisasjonClient = wsClientBuilder.createSOAPClient(
            endpointSTSClientConfig,
            env.organisasjonEndpointUrl,
            OrganisasjonV5::class.java,
            OrganisasjonClient::class.java)

    val sakOgBehandlingClient: SakOgBehandlingClient = wsClientBuilder.createSOAPClient(
            endpointSTSClientConfig,
            env.sakOgBehandlingEndpointUrl,
            SakOgBehandlingV1::class.java,
            SakOgBehandlingClient::class.java)
}

fun <S, T> WsClientBuilder.createSOAPClient(sts: EndpointSTSClientConfig, serviceLocation: String, soapInterface: Class<S>, clientInterface: Class<T>): T {
    val port: S = createPort(serviceLocation, soapInterface)
    sts.configureRequestSamlToken(port, EndpointSTSClientConfig.STS_SAML_POLICY)
    @Suppress("UNCHECKED_CAST") // if T's constructor doesn't produce a T, then we have bigger problems than a class cast exception
    return clientInterface.constructors[0].newInstance(port) as T
}