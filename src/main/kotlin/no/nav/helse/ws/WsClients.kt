package no.nav.helse.ws

import no.nav.helse.http.aktør.AktørregisterClient
import no.nav.helse.sts.StsRestClient
import no.nav.helse.ws.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.helse.ws.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.ws.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import no.nav.helse.ws.inntekt.InntektClient
import no.nav.helse.ws.meldekort.MeldekortClient
import no.nav.helse.ws.organisasjon.OrganisasjonClient
import no.nav.helse.ws.person.PersonClient
import no.nav.helse.ws.sakogbehandling.SakOgBehandlingClient
import no.nav.helse.ws.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.helse.ws.sts.configureFor
import no.nav.helse.ws.sykepenger.SykepengerClient
import org.apache.cxf.ws.security.trust.STSClient

class WsClients(private val stsClientWs: STSClient, private val stsClientRest: StsRestClient, private val allowInsecureRequests: Boolean = false) {

    fun aktør(endpointUrl: String) = AktørregisterClient(endpointUrl, stsClientRest)

    fun organisasjon(endpointUrl: String): OrganisasjonClient {
        val port = SoapPorts.OrganisasjonV5(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return OrganisasjonClient(port)
    }

    fun person(endpointUrl: String): PersonClient {
        val port = SoapPorts.PersonV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return PersonClient(port)
    }

    fun arbeidsfordeling(endpointUrl: String): ArbeidsfordelingClient {
        val port = SoapPorts.ArbeidsfordelingV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return ArbeidsfordelingClient(port)
    }

    fun inntekt(endpointUrl: String): InntektClient {
        val port = SoapPorts.InntektV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return InntektClient(port)
    }

    fun arbeidsforhold(endpointUrl: String): ArbeidsforholdClient {
        val port = SoapPorts.ArbeidsforholdV3(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }

        return ArbeidsforholdClient(port)
    }

    fun sakOgBehandling(endpointUrl: String): SakOgBehandlingClient {
        val port = SoapPorts.SakOgBehandlingV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return SakOgBehandlingClient(port)
    }

    fun sykepengeliste(endpointUrl: String): SykepengerClient {
        val port = SoapPorts.SykepengerV2(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return SykepengerClient(port)
    }

    fun meldekort(endpointUrl: String): MeldekortClient {
        val port = SoapPorts.MeldekortUtbetalingsgrunnlagV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return MeldekortClient(port)
    }

    fun infotrygdBeregningsgrunnlag(endpointUrl: String): InfotrygdBeregningsgrunnlagListeClient {
        val port = SoapPorts.InfotrygdBeregningsgrunnlagV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return InfotrygdBeregningsgrunnlagListeClient(port)
    }

}
