package no.nav.helse.ws.arbeidsforhold

import io.grpc.stub.StreamObserver
import no.nav.helse.Failure
import no.nav.helse.OppslagResult
import no.nav.helse.Success
import no.nav.helse.oppslag.arbeidsforhold.Arbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdServiceGrpc.ArbeidsforholdServiceImplBase
import no.nav.helse.oppslag.arbeidsforhold.CollectionOfArbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.FinnArbeidsforholdForArbeidstager
import no.nav.helse.ws.Fødselsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse
import javax.xml.datatype.XMLGregorianCalendar

class ArbeidsforholdGrpc(val arbeidsforholdClient: ArbeidsforholdClient) : ArbeidsforholdServiceImplBase() {
    override fun finnArbeidsforhold(request: FinnArbeidsforholdForArbeidstager, responseObserver: StreamObserver<CollectionOfArbeidsforhold>) {
        val oppslagResult: OppslagResult = arbeidsforholdClient.finnArbeidsforholdForFnr(Fødselsnummer(request.norskIdent))

        when (oppslagResult) {
            is Success<*> -> {
                val result: FinnArbeidsforholdPrArbeidstakerResponse = (oppslagResult.data as FinnArbeidsforholdPrArbeidstakerResponse)
                responseObserver.onNext(result.asProto())
                responseObserver.onCompleted()
            }
            is Failure -> {
                responseObserver.onError(Throwable("that didn't go so well..."))
            }
        }
    }
}

private fun FinnArbeidsforholdPrArbeidstakerResponse.asProto(): CollectionOfArbeidsforhold {
    val collectionOfArbeidsforholdBuilder: CollectionOfArbeidsforhold.Builder = CollectionOfArbeidsforhold.newBuilder()
    this.arbeidsforhold.map { arbeidsforholdSOAP ->
        Arbeidsforhold.newBuilder()
                .setArbeidsgiver(arbeidsforholdSOAP.arbeidsgiver.aktoerId)
                .setArbeidsforholdId(arbeidsforholdSOAP.arbeidsforholdID)
                .setArbeidsforholdType(arbeidsforholdSOAP.arbeidsforholdstype.kodeverksRef)
                .setFomISO8601(arbeidsforholdSOAP.ansettelsesPeriode.fomBruksperiode.iso8601())
                .setTomISO8601(arbeidsforholdSOAP.ansettelsesPeriode.tomBruksperiode.iso8601())
    }.forEach { builder -> collectionOfArbeidsforholdBuilder.addArbeidsforhold(builder.build()) }
    return collectionOfArbeidsforholdBuilder.build()
}

fun XMLGregorianCalendar.iso8601(): String {
    val padYear = "${this.year}".padStart(4, '0')
    val padMonth = "${this.month}".padStart(2, '0')
    val padDay = "${this.day}".padStart(2, '0')
    return "$padYear-$padMonth-$padDay"
}
