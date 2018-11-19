package no.nav.helse.ws.sakogbehandling

import io.prometheus.client.Counter
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.FinnSakOgBehandlingskjedeListe
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse
import org.slf4j.LoggerFactory

class SakOgBehandlingClient(private val sakOgBehandling: SakOgBehandlingV1) {

    private val log = LoggerFactory.getLogger("SakOgBehandlingClient")

    private val counter = Counter.build()
            .name("oppslag_sak_og_behandling")
            .labelNames("status")
            .help("Antall registeroppslag av sak og tilhørende behandlingskjeder")
            .register()

    fun finnSakOgBehandling(aktorId: String): OppslagResult {
        val request = FinnSakOgBehandlingskjedeListeRequest()
                .apply { this.aktoerREF = aktorId }
                .apply { this.isKunAapneBehandlingskjeder = true }

        return try {
            val remoteResult: FinnSakOgBehandlingskjedeListeResponse? = sakOgBehandling.finnSakOgBehandlingskjedeListe(request)
            counter.labels("success").inc()
            Success(remoteResult)
        } catch (ex: Exception) {
            log.error("Error while doing arbeidsforhold lookup", ex)
            counter.labels("failure").inc()
            Failure(listOf(ex.message ?: "unknown error"))
        }
    }
}
