package no.nav.helse.ws.sykepenger

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.Test
import java.time.Month
import java.util.*
import javax.xml.datatype.DatatypeFactory

class XMLGregorianCalendarTest {

    @Test
    fun `make sure conversion isn't stupid`() {
        val cal = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2001, Month.JANUARY.value, 21, TimeZone.SHORT)

        val localDate = cal.toLocalDate()

        assertThat(localDate.month, equalTo(Month.JANUARY))
    }
}