package no.nav.helse.ws

import no.nav.helse.Environment
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean

class Clients(env: Environment) {
    private val stsClient = stsClient(env.securityTokenServiceEndpointUrl,
            env.securityTokenUsername to env.securityTokenPassword
    )
    val personClient = PersonClient(createServicePort(env.personEndpointUrl, PersonV3::class.java)
            .apply(stsClient::configureFor))

    val inntektClient = InntektClient(createServicePort(env.inntektEndpointUrl, InntektV3::class.java)
            .apply(stsClient::configureFor))

    val arbeidsforholdClient = ArbeidsforholdClient(createServicePort(env.arbeidsforholdEndpointUrl, ArbeidsforholdV3::class.java)
            .apply(stsClient::configureFor))

    val organisasjonClient = OrganisasjonClient(createServicePort(env.organisasjonEndpointUrl, OrganisasjonV5::class.java)
            .apply(stsClient::configureFor))

    val sakOgBehandlingClient = SakOgBehandlingClient(createServicePort(env.sakOgBehandlingEndpointUrl, SakOgBehandlingV1::class.java)
            .apply(stsClient::configureFor))

    private fun <PORT_TYPE> createServicePort(serviceUrl: String, service: Class<PORT_TYPE>): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            serviceClass = service
            features = listOf(LoggingFeature())
        }

        @Suppress("UNCHECKED_CAST")
        return factory.create() as PORT_TYPE
    }
}
