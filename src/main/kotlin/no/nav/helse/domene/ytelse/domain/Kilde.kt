package no.nav.helse.domene.ytelse.domain

sealed class Kilde {

    object Arena: Kilde()
    object Infotrygd: Kilde()

    fun type() = when (this) {
        is Arena -> "Arena"
        is Infotrygd -> "Infotrygd"
    }
}
