package no.nav.helse.ws

import no.nav.helse.Environment
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sts.STS_SAML_POLICY
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sts.stsClient
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.inntekt.v3.InntektV3
import no.nav.tjeneste.virksomhet.organisasjon.v5.OrganisasjonV5
import no.nav.tjeneste.virksomhet.person.v3.PersonV3
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.SakOgBehandling_v1PortType
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean

class Clients(val env: Environment, val policyUri: String = STS_SAML_POLICY) {
    private val stsClient get() = stsClient(env.securityTokenServiceEndpointUrl,
            env.securityTokenUsername to env.securityTokenPassword
    )
    val personClient get() = PersonClient {createServicePort(env.personEndpointUrl, PersonV3::class.java)
            .apply{stsClient.configureFor(this, policyUri)}}

    val inntektClient get() = InntektClient {createServicePort(env.inntektEndpointUrl, InntektV3::class.java)
            .apply{stsClient.configureFor(this, policyUri)}}

    val arbeidsforholdClient get() = ArbeidsforholdClient {createServicePort(env.arbeidsforholdEndpointUrl, ArbeidsforholdV3::class.java)
            .apply{stsClient.configureFor(this, policyUri)}}

    val organisasjonClient get() = OrganisasjonClient{createServicePort(env.organisasjonEndpointUrl, OrganisasjonV5::class.java)
            .apply{stsClient.configureFor(this, policyUri)}}

    val sakOgBehandlingClient get() = SakOgBehandlingClient{createServicePort(env.sakOgBehandlingEndpointUrl, SakOgBehandling_v1PortType::class.java)
            .apply{stsClient.configureFor(this, policyUri)}}

    companion object {
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
}
