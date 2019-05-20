package no.nav.helse.oppslag

import no.nav.helse.oppslag.aktør.AktørregisterClient
import no.nav.helse.oppslag.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagListeClient
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.helse.oppslag.person.PersonClient
import no.nav.helse.oppslag.sts.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.helse.oppslag.sts.configureFor
import no.nav.helse.sts.StsRestClient
import org.apache.cxf.ws.security.trust.STSClient

class WsClients(private val stsClientWs: STSClient, private val stsClientRest: StsRestClient, private val allowInsecureRequests: Boolean = false) {

    fun aktør(endpointUrl: String) = AktørregisterClient(endpointUrl, stsClientRest)

    fun meldekortUtbetalingsgrunnlag(endpointUrl: String): MeldekortUtbetalingsgrunnlagClient {
        val port = SoapPorts.MeldekortUtbetalingsgrunnlagV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return MeldekortUtbetalingsgrunnlagClient(port)
    }

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

    fun infotrygdSak(endpointUrl: String): InfotrygdSakClient {
        val port = SoapPorts.InfotrygdSakV1(endpointUrl).apply {
            if (allowInsecureRequests) {
                stsClientWs.configureFor(this, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
            } else {
                stsClientWs.configureFor(this)
            }
        }
        return InfotrygdSakClient(port)
    }
}
