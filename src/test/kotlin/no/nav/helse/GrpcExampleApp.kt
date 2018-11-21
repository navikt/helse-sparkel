package no.nav.helse

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import no.nav.helse.oppslag.arbeidsforhold.ArbeidsforholdServiceGrpc
import no.nav.helse.oppslag.arbeidsforhold.CollectionOfArbeidsforhold
import no.nav.helse.oppslag.arbeidsforhold.FinnArbeidsforholdForArbeidstager

/**
 * this obviously requires a running server that in turn has the right enpoints and credentials to
 * do actual lookups. (or at the very least fake lookups)
 */
fun main() {
    val host = "localhost"
    val port = 8081
    val fakeIdent = "01010112345"

    // usePlaintext turns off ssl/tsl.
    val managedChannel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

    val stub: ArbeidsforholdServiceGrpc.ArbeidsforholdServiceBlockingStub = ArbeidsforholdServiceGrpc.newBlockingStub(managedChannel)

    println("attempting to contact a Grpc-server at $host:$port")

    val result: CollectionOfArbeidsforhold = stub.finnArbeidsforhold(
            FinnArbeidsforholdForArbeidstager
                    .newBuilder()
                    .setNorskIdent(fakeIdent)
                    .build())

    println("result: $result")
}