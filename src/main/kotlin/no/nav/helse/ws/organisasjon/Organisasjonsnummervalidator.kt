package no.nav.helse.ws.organisasjon

object Organisasjonsnummervalidator {

    fun erGyldig(orgnummer: String): Boolean {
        if (orgnummer.length != 9) {
            return false
        }

        val sisteSiffer = orgnummer[orgnummer.length - 1]

        return try {
            Mod11.kontrollsiffer(orgnummer.substring(0, orgnummer.lastIndex)) == sisteSiffer
        } catch (err: Exception) {
            false
        }
    }
}
