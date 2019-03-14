package no.nav.helse.ws.sykepenger

import org.json.JSONObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HentSykepengeListeTest {

    @Test
    fun `get something simple`() {
        val vedtak = JSONObject(json).toListOfSykepengerPeriode()
        assertEquals(vedtak.size, 13)
    }

}

val json = """{
  "sykmeldingsperioder": [
    {
      "arbeidsforholdList": [
        {
          "arbeidsgiverKontonr": "0312",
          "arbeidsgiverNavn": "0312"
        },
        {
          "arbeidsgiverKontonr": "0312",
          "arbeidsgiverNavn": "0312"
        }
      ],
      "ident": 31266100932216,
      "seq": 79899795,
      "sykemeldtFom": "2010-02-04",
      "tknr": "0312",
      "utbetalingList": [
        {
          "dagsats": 1152.0,
          "fom": "2010-09-01",
          "oppgjorsType": "51",
          "periodeType": "7",
          "tom": "2010-09-08",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-09-20"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-07-01",
          "oppgjorsType": "31",
          "periodeType": "0",
          "tom": "2010-09-08",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-10-06"
        },
        {
          "dagsats": 0.0,
          "fom": "2010-07-01",
          "oppgjorsType": "41",
          "periodeType": "4",
          "tom": "2010-09-08",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-10-26"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-06-21",
          "oppgjorsType": "51",
          "periodeType": "7",
          "tom": "2010-08-31",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-09-10"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-06-21",
          "oppgjorsType": "50",
          "periodeType": "5",
          "tom": "2010-06-30",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-10-06"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-05-25",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2010-06-20",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-06-18"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-04-12",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2010-05-24",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-05-31"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-03-31",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2010-04-11",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-04-27"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-02-22",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2010-03-30",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-04-23"
        },
        {
          "dagsats": 1152.0,
          "fom": "2010-02-16",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2010-02-19",
          "utbetalingsGrad": "100",
          "utbetalt": "2010-02-22"
        }
      ]
    },
    {
      "arbeidsforholdList": [
        {
          "arbeidsgiverKontonr": "0312",
          "arbeidsgiverNavn": "0312"
        },
        {
          "arbeidsgiverKontonr": "0312",
          "arbeidsgiverNavn": "0312"
        }
      ],
      "ident": 31266100932216,
      "seq": 79909572,
      "sykemeldtFom": "2009-04-27",
      "tknr": "0312",
      "utbetalingList": [
        {
          "dagsats": 1523.0,
          "fom": "2009-07-27",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2009-09-30",
          "utbetalingsGrad": "100",
          "utbetalt": "2009-09-28"
        },
        {
          "dagsats": 1523.0,
          "fom": "2009-06-22",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2009-07-26",
          "utbetalingsGrad": "100",
          "utbetalt": "2009-09-21"
        },
        {
          "dagsats": 1523.0,
          "fom": "2009-05-13",
          "oppgjorsType": "51",
          "periodeType": "5",
          "tom": "2009-06-21",
          "utbetalingsGrad": "100",
          "utbetalt": "2009-07-20"
        }
      ]
    }
  ]
}
""".trimIndent()