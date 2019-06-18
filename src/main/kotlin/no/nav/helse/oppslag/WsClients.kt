package no.nav.helse.oppslag

import no.nav.cxf.metrics.MetricFeature
import no.nav.helse.callIdGenerator
import no.nav.helse.oppslag.aktør.AktørregisterClient
import no.nav.helse.oppslag.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.helse.oppslag.arbeidsfordeling.ArbeidsfordelingFactory
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdClient
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdFactory
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagClient
import no.nav.helse.oppslag.arena.MeldekortUtbetalingsgrunnlagFactory
import no.nav.helse.oppslag.infotrygd.InfotrygdSakClient
import no.nav.helse.oppslag.infotrygd.InfotrygdSakFactory
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagClient
import no.nav.helse.oppslag.infotrygdberegningsgrunnlag.InfotrygdBeregningsgrunnlagFactory
import no.nav.helse.oppslag.inntekt.InntektClient
import no.nav.helse.oppslag.inntekt.InntektFactory
import no.nav.helse.oppslag.organisasjon.OrganisasjonClient
import no.nav.helse.oppslag.organisasjon.OrganisasjonFactory
import no.nav.helse.oppslag.person.PersonClient
import no.nav.helse.oppslag.person.PersonFactory
import no.nav.helse.oppslag.sts.configureFor
import no.nav.helse.sts.StsRestClient
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.security.trust.STSClient

class WsClients(private val stsClientWs: STSClient,
                private val stsClientRest: StsRestClient,
                private val callIdGenerator: () -> String) {

    private val features get() = listOf(WSAddressingFeature(), LoggingFeature(), MetricFeature())
    private val outInterceptors get() = listOf(CallIdInterceptor(callIdGenerator))

    companion object {
        init {
            System.setProperty("javax.xml.soap.SAAJMetaFactory", "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl")
        }
    }

    private fun <ServicePort : Any> ServicePort.withSts() =
            apply {
                stsClientWs.configureFor(this)
            }

    fun aktør(endpointUrl: String) =
            AktørregisterClient(endpointUrl, stsClientRest)

    fun meldekortUtbetalingsgrunnlag(endpointUrl: String) =
            MeldekortUtbetalingsgrunnlagFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        MeldekortUtbetalingsgrunnlagClient(port)
                    }

    fun organisasjon(endpointUrl: String) =
            OrganisasjonFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        OrganisasjonClient(port)
                    }

    fun person(endpointUrl: String) =
            PersonFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        PersonClient(port)
                    }

    fun arbeidsfordeling(endpointUrl: String) =
            ArbeidsfordelingFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        ArbeidsfordelingClient(port)
                    }

    fun inntekt(endpointUrl: String) =
            InntektFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        InntektClient(port)
                    }

    fun arbeidsforhold(endpointUrl: String) =
            ArbeidsforholdFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        ArbeidsforholdClient(port)
                    }

    fun infotrygdBeregningsgrunnlag(endpointUrl: String) =
            InfotrygdBeregningsgrunnlagFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        InfotrygdBeregningsgrunnlagClient(port)
                    }

    fun infotrygdSak(endpointUrl: String) =
            InfotrygdSakFactory.create(endpointUrl, features, outInterceptors)
                    .withSts().let { port ->
                        InfotrygdSakClient(port)
                    }
}
