package no.nav.helse.domene.ytelse.domain

sealed class Tema {

    object Sykepenger: Tema()
    object Foreldrepenger: Tema()
    object Engangsstønad: Tema()
    object PårørendeSykdom: Tema()
    object EnsligForsørger: Tema()
    object Ukjent: Tema()

    fun name() = when (this) {
        is Sykepenger -> "Sykepenger"
        is Foreldrepenger -> "Foreldrepenger"
        is Engangsstønad -> "Engangsstønad"
        is PårørendeSykdom -> "PårørendeSykdom"
        is EnsligForsørger -> "EnsligForsørger"
        is Ukjent -> "Ukjent"
    }

    companion object {
        fun fraKode(tema: String) =
                when (tema) {
                    "SP" -> Tema.Sykepenger
                    "FA" -> Tema.Foreldrepenger
                    "BS" -> Tema.PårørendeSykdom
                    "EF" -> Tema.EnsligForsørger
                    else -> Tema.Ukjent
                }
    }
}
