package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StsRestClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        configureFor(server.port())
    }

    @Test
    fun `should parse a token successfully`() {
        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("default")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        val token: String = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar").token()
        assertEquals("default access token", token)
    }

    @Test
    fun `should cache tokens`() {
        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("caching")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        stubFor(stsRequestMapping
                .willReturn(ok(bad_token))
                .inScenario("caching")
                .whenScenarioStateIs("token acquired")
        )

        val authHelper = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")
        val first = authHelper.token()

        val second: String = authHelper.token()

        assertEquals(first, second)
        assertEquals("default access token", second)
    }

    @Test
    fun `should get new token when old has expired`() {
        stubFor(stsRequestMapping
                .willReturn(ok(short_lived_token))
                .inScenario("expiry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("expired token sent"))

        stubFor(stsRequestMapping
                .willReturn(ok(default_token))
                .inScenario("expiry")
                .whenScenarioStateIs("expired token sent")
        )

        val client = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")

        // get the short-lived one
        val token1 = client.token()

        // get the new one
        val token2 = client.token()

        assertNotEquals(token1, token2)
        assertEquals("default access token", token2)
    }

    @Test
    fun `should parse a saml token successfully`() {
        stubFor(stsSamlRequestMapping
                .willReturn(ok(default_saml_token))
                .inScenario("default_saml")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        val token: String = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar").samlToken()
        assertTrue(token.contains("<saml2:Assertion"))
        assertTrue(token.contains("</saml2:Assertion>"))
    }

    @Test
    fun `should cache saml token`() {
        stubFor(stsSamlRequestMapping
                .willReturn(ok(default_saml_token))
                .inScenario("cache_saml")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        stubFor(stsSamlRequestMapping
                .willReturn(ok(bad_saml_token))
                .inScenario("cache_saml")
                .whenScenarioStateIs("token acquired")
                .willSetStateTo("token acquired"))

        val client = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")

        assertEquals(client.samlToken(), client.samlToken())
    }

    @Test
    fun `should throw exception when token is invalid base64`() {
        stubFor(stsSamlRequestMapping
                .willReturn(ok(bad_saml_token))
                .inScenario("bad_saml")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        try {
            StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar").samlToken()
            fail<Any>("expected samlToken() to throw IllegalArgumentException when token is not base64")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `should get new saml token when old has expired`() {
        stubFor(stsSamlRequestMapping
                .willReturn(ok(short_lived_saml_token))
                .inScenario("saml_expiry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("expired token sent"))

        stubFor(stsSamlRequestMapping
                .willReturn(ok(default_saml_token))
                .inScenario("saml_expiry")
                .whenScenarioStateIs("expired token sent")
        )

        val client = StsRestClient(baseUrl = server.baseUrl(), username = "foo", password = "bar")

        // get the short-lived one val
        val token1 = client.samlToken()

        // get the new one
        val token2 = client.samlToken()

        println(token1)

        assertTrue(token1.contains("<saml2:Conditions NotBefore=\"2018-11-28T09:53:02.322Z\" NotOnOrAfter=\"2018-11-28T10:53:02.322Z\"/>"))

        assertNotEquals(token1, token2)

        assertTrue(token2.contains("<saml2:Conditions NotBefore=\"2018-11-28T08:53:02.322Z\" NotOnOrAfter=\"2018-11-28T09:53:02.322Z\"/>"))
    }
}

private val stsRequestMapping: MappingBuilder = get(urlPathEqualTo("/rest/v1/sts/token"))
        .withQueryParam("grant_type", equalTo("client_credentials"))
        .withQueryParam("scope", equalTo("openid"))
        .withBasicAuth("foo", "bar")
        .withHeader("Accept", equalTo("application/json"))

private val stsSamlRequestMapping: MappingBuilder = get(urlPathEqualTo("/rest/v1/sts/samltoken"))
        .withBasicAuth("foo", "bar")
        .withHeader("Accept", equalTo("application/json"))

private val default_token = """{
  "access_token": "default access token",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()

private val short_lived_token = """{
  "access_token": "short lived token",
  "token_type": "Bearer",
  "expires_in": 1
}""".trimIndent()

private val bad_token = """{
  "access_token": "this token shouldn't be requested",
  "token_type": "Bearer",
  "expires_in": 1000000000000
}""".trimIndent()

private val default_saml_token = """{
  "access_token": "PHNhbWwyOkFzc2VydGlvbiB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiIgSUQ9IlNBTUwtNzlkMGZiOWUtYmY2OC00YjU4LWIxYTYtYzIzYjk4ZmJmYmU3IiBJc3N1ZUluc3RhbnQ9IjIwMTgtMTEtMjhUMDg6NTM6MDIuMzIyWiIgVmVyc2lvbj0iMi4wIj48c2FtbDI6SXNzdWVyPklTMDI8L3NhbWwyOklzc3Vlcj48U2lnbmF0dXJlIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48U2lnbmVkSW5mbz48Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48UmVmZXJlbmNlIFVSST0iI1NBTUwtNzlkMGZiOWUtYmY2OC00YjU4LWIxYTYtYzIzYjk4ZmJmYmU3Ij48VHJhbnNmb3Jtcz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmUiLz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8-PC9UcmFuc2Zvcm1zPjxEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjc2hhMSIvPjxEaWdlc3RWYWx1ZT42dVlWSmp0WmlndG1IUHVjTE00UUZ2dzRBNTg9PC9EaWdlc3RWYWx1ZT48L1JlZmVyZW5jZT48L1NpZ25lZEluZm8-PFNpZ25hdHVyZVZhbHVlPkUrUDFvVko2WHZWSE84blhqeE1LdHlxclVSVUt0NW1zMTRvWVhPZDdiK3BKRWd3Q29KOXE4Q3RuU3BoNzZ0ZVhVbTQzUXA2NFBXdnIKak84Q0NkN01vdUsyY0d3cnVXYThMRm1teTJNM2psV21JSWdWRkM4eUIxR1NUU0xIZUpDQ2lTSjM1WUFqbzVHYVV0UzVEQlBqSUtvQQpBRHIwL2V0Qm9mbktiZk5jOCtPODZNWFhTK01DdC9zR2RYcGFqNngrbUkrcTNjT2F0OFN3TVhBVXNHWHBkNVBCTlBQR1B1SjBjTkEyCmJwaUJRdU9sRVZ1SmxVVWgzemR0aUYrRy9DYWpmOERIYXE2cjdpVThteUpKdnNmMWpRTmx2Ky91MW84OUdoOWlycWR6N3hnZ3pHWloKMHUxeWFUdVlOUkVIMjA3OHoyS2htN0RMRTNwTysxVWR3K1BSalE9PTwvU2lnbmF0dXJlVmFsdWU-PEtleUluZm8-PFg1MDlEYXRhPjxYNTA5Q2VydGlmaWNhdGU-TUlJR3dqQ0NCYXFnQXdJQkFnSVRhZ0FBSExhR1g1UFpIVEZ2YkFBQkFBQWN0akFOQmdrcWhraUc5dzBCQVFzRkFEQlFNUlV3RXdZSwpDWkltaVpQeUxHUUJHUllGYkc5allXd3hGekFWQmdvSmtpYUprL0lzWkFFWkZnZHdjbVZ3Y205a01SNHdIQVlEVlFRREV4VkNNamNnClNYTnpkV2x1WnlCRFFTQkpiblJsY200d0hoY05NVGd4TURBME1URTFPRFUyV2hjTk1qQXhNREEwTVRJd09EVTJXakIrTVJVd0V3WUsKQ1pJbWlaUHlMR1FCR1JZRmJHOWpZV3d4RnpBVkJnb0praWFKay9Jc1pBRVpGZ2R3Y21Wd2NtOWtNUmd3RmdZRFZRUUxFdzlUWlhKMgphV05sUVdOamIzVnVkSE14RlRBVEJnTlZCQXNUREVGd2NHeEJZMk52ZFc1MGN6RWJNQmtHQTFVRUF4TVNjM0oyYzJWamRYSnBkSGt0CmRHOXJaVzR0TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFrZktVcUZVbjZDWnpsUkpRNVlLOFhGU1gKTHV6amVGWUN2eTdiOWVqeUE5eUVJdmNXalZKMnViYVpQNmhzbWxmdGRVUlBTOTBadFhLajIvL05UYjZGK2FZYjdWQ3JXb2N2MFNDZAo5WDMrYkh5S09lTEQ1b3ovd2RhaWdyQmZ4eGVUajF5WHNBZ3UzdHF5VDhJNnkyaWlCQjV6ZzlmS2xjazRnNko1cjdCTytiZG9wWUZlCkI1eDJ5bnZFY2pwM0d3OERHMlA5SCtSTlpycDVFdzJqdHY4aW0zV01Dc28zL1FiL0U3THN2Y1dDbmpXTkxRSElrTUh6OXJhb013ZGYKcnBzbnRSN1NaZDVFUllsdi96enBXSjF5MjNmRFljTlF1empTOW13Z2ZWYU9lZlpFRnlSNE9rdUpZK1lmaFoydWZxc0dmdmd1L3NMOApQVFBjL082RUdWTVNBUUlEQVFBQm80SURaVENDQTJFd0hRWURWUjBPQkJZRUZCUklTRE91cXZSRGY1ZS9GNXRma0s4c1BaWnpNQjhHCkExVWRJd1FZTUJhQUZPTm9ZMVc5MjJqYk56WGtZS2xTQjZzZ21xdU5NSUlCSVFZRFZSMGZCSUlCR0RDQ0FSUXdnZ0VRb0lJQkRLQ0MKQVFpR2djZHNaR0Z3T2k4dkwyTnVQVUl5TnlVeU1FbHpjM1ZwYm1jbE1qQkRRU1V5TUVsdWRHVnliaXhEVGoxQ01qZEVVbFpYTURBNApMRU5PUFVORVVDeERUajFRZFdKc2FXTWxNakJyWlhrbE1qQlRaWEoyYVdObGN5eERUajFUWlhKMmFXTmxjeXhEVGoxRGIyNW1hV2QxCmNtRjBhVzl1TEVSRFBYQnlaWEJ5YjJRc1JFTTliRzlqWVd3L1kyVnlkR2xtYVdOaGRHVlNaWFp2WTJGMGFXOXVUR2x6ZEQ5aVlYTmwKUDI5aWFtVmpkRU5zWVhOelBXTlNURVJwYzNSeWFXSjFkR2x2YmxCdmFXNTBoanhvZEhSd09pOHZZM0pzTG5CeVpYQnliMlF1Ykc5agpZV3d2UTNKc0wwSXlOeVV5TUVsemMzVnBibWNsTWpCRFFTVXlNRWx1ZEdWeWJpNWpjbXd3Z2dGakJnZ3JCZ0VGQlFjQkFRU0NBVlV3CmdnRlJNSUc4QmdnckJnRUZCUWN3QW9hQnIyeGtZWEE2THk4dlkyNDlRakkzSlRJd1NYTnpkV2x1WnlVeU1FTkJKVEl3U1c1MFpYSnUKTEVOT1BVRkpRU3hEVGoxUWRXSnNhV01sTWpCclpYa2xNakJUWlhKMmFXTmxjeXhEVGoxVFpYSjJhV05sY3l4RFRqMURiMjVtYVdkMQpjbUYwYVc5dUxFUkRQWEJ5WlhCeWIyUXNSRU05Ykc5allXdy9ZMEZEWlhKMGFXWnBZMkYwWlQ5aVlYTmxQMjlpYW1WamRFTnNZWE56ClBXTmxjblJwWm1sallYUnBiMjVCZFhSb2IzSnBkSGt3S2dZSUt3WUJCUVVITUFHR0htaDBkSEE2THk5dlkzTndMbkJ5WlhCeWIyUXUKYkc5allXd3ZiMk56Y0RCa0JnZ3JCZ0VGQlFjd0FvWllhSFIwY0RvdkwyTnliQzV3Y21Wd2NtOWtMbXh2WTJGc0wwTnliQzlDTWpkRQpVbFpYTURBNExuQnlaWEJ5YjJRdWJHOWpZV3hmUWpJM0pUSXdTWE56ZFdsdVp5VXlNRU5CSlRJd1NXNTBaWEp1S0RFcExtTnlkREFPCkJnTlZIUThCQWY4RUJBTUNCYUF3T3dZSkt3WUJCQUdDTnhVSEJDNHdMQVlrS3dZQkJBR0NOeFVJZ2RiVlhJT0FwMXlFOVowa202UlQKb0xKNWdTU0h2cDFGa1lNaUFnRmtBZ0VDTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjREFqQW5CZ2tyQmdFRQpBWUkzRlFvRUdqQVlNQW9HQ0NzR0FRVUZCd01CTUFvR0NDc0dBUVVGQndNQ01BMEdDU3FHU0liM0RRRUJDd1VBQTRJQkFRQ2xNQjhVCnZiQ1lLRlBJWFRPSzd4SmdKVDJPNUtGWkFMKytURGZaTVA4elF6VkQvcjBvZktrNmlGSzArdElrU0RlSUdHMy9OWk1Dd0dOMDRySE4KTWlpK2hwUThkRDl0Ym85eGtoVC9waElNVFU2YnVZc1BMcjltYWx4Wi9PWjZkcHdzclU4US9YM2NTekwyMEphZFY2TVVDaDY4Tit0RApTQzVjZXhPM2MwVFVUdGdhN0pJNGp0b0hQTms4K1FxVTREbXpJbVRtSlpjUGJJOU41bUVJNHFqaVN4Uk5KRkp3dzhHRkdlb1dRYklaClJNRkxvbmRsVTdNV1FwL2R2eFVzU0V3Nk9DQnFDSytOWFdUK1VnYUdUbGlRRVN0UGc5aElMT0VQOXlmKy9HVmpqalZLOEd2Mmg5aVIKYlNDZ0dxVWxTMVZZNnVQc3YwVUZXR0RpMGdzM3YrZUo8L1g1MDlDZXJ0aWZpY2F0ZT48WDUwOUlzc3VlclNlcmlhbD48WDUwOUlzc3Vlck5hbWU-Q049QjI3IElzc3VpbmcgQ0EgSW50ZXJuLCBEQz1wcmVwcm9kLCBEQz1sb2NhbDwvWDUwOUlzc3Vlck5hbWU-PFg1MDlTZXJpYWxOdW1iZXI-MjM2Mzg3OTAyOTIxMDM1MzM3ODU1Mjg2ODU5MjY5MDEwOTM2MjY3MjI0NTk0MjwvWDUwOVNlcmlhbE51bWJlcj48L1g1MDlJc3N1ZXJTZXJpYWw-PC9YNTA5RGF0YT48L0tleUluZm8-PC9TaWduYXR1cmU-PHNhbWwyOlN1YmplY3Q-PHNhbWwyOk5hbWVJRCBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjEuMTpuYW1laWQtZm9ybWF0OnVuc3BlY2lmaWVkIj5zcnZzcGlubmU8L3NhbWwyOk5hbWVJRD48c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiPjxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9uRGF0YSBOb3RCZWZvcmU9IjIwMTgtMTEtMjhUMDg6NTM6MDIuMzIyWiIgTm90T25PckFmdGVyPSIyMDE4LTExLTI4VDA5OjUzOjAyLjMyMloiLz48L3NhbWwyOlN1YmplY3RDb25maXJtYXRpb24-PC9zYW1sMjpTdWJqZWN0PjxzYW1sMjpDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOC0xMS0yOFQwODo1MzowMi4zMjJaIiBOb3RPbk9yQWZ0ZXI9IjIwMTgtMTEtMjhUMDk6NTM6MDIuMzIyWiIvPjxzYW1sMjpBdHRyaWJ1dGVTdGF0ZW1lbnQ-PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJpZGVudFR5cGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dXJpIj48c2FtbDI6QXR0cmlidXRlVmFsdWU-U3lzdGVtcmVzc3Vyczwvc2FtbDI6QXR0cmlidXRlVmFsdWU-PC9zYW1sMjpBdHRyaWJ1dGU-PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJhdXRoZW50aWNhdGlvbkxldmVsIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OnVyaSI-PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPjA8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjxzYW1sMjpBdHRyaWJ1dGUgTmFtZT0iY29uc3VtZXJJZCIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYWx1ZT5zcnZzcGlubmU8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjwvc2FtbDI6QXR0cmlidXRlU3RhdGVtZW50Pjwvc2FtbDI6QXNzZXJ0aW9uPg",
  "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
  "token_type": "Bearer",
  "expires_in": 3599
}"""

private val short_lived_saml_token = """{
  "access_token": "PHNhbWwyOkFzc2VydGlvbiB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiIgSUQ9IlNBTUwtNzlkMGZiOWUtYmY2OC00YjU4LWIxYTYtYzIzYjk4ZmJmYmU3IiBJc3N1ZUluc3RhbnQ9IjIwMTgtMTEtMjhUMDg6NTM6MDIuMzIyWiIgVmVyc2lvbj0iMi4wIj48c2FtbDI6SXNzdWVyPklTMDI8L3NhbWwyOklzc3Vlcj48U2lnbmF0dXJlIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48U2lnbmVkSW5mbz48Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48UmVmZXJlbmNlIFVSST0iI1NBTUwtNzlkMGZiOWUtYmY2OC00YjU4LWIxYTYtYzIzYjk4ZmJmYmU3Ij48VHJhbnNmb3Jtcz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmUiLz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8+PC9UcmFuc2Zvcm1zPjxEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjc2hhMSIvPjxEaWdlc3RWYWx1ZT42dVlWSmp0WmlndG1IUHVjTE00UUZ2dzRBNTg9PC9EaWdlc3RWYWx1ZT48L1JlZmVyZW5jZT48L1NpZ25lZEluZm8+PFNpZ25hdHVyZVZhbHVlPkUrUDFvVko2WHZWSE84blhqeE1LdHlxclVSVUt0NW1zMTRvWVhPZDdiK3BKRWd3Q29KOXE4Q3RuU3BoNzZ0ZVhVbTQzUXA2NFBXdnIKak84Q0NkN01vdUsyY0d3cnVXYThMRm1teTJNM2psV21JSWdWRkM4eUIxR1NUU0xIZUpDQ2lTSjM1WUFqbzVHYVV0UzVEQlBqSUtvQQpBRHIwL2V0Qm9mbktiZk5jOCtPODZNWFhTK01DdC9zR2RYcGFqNngrbUkrcTNjT2F0OFN3TVhBVXNHWHBkNVBCTlBQR1B1SjBjTkEyCmJwaUJRdU9sRVZ1SmxVVWgzemR0aUYrRy9DYWpmOERIYXE2cjdpVThteUpKdnNmMWpRTmx2Ky91MW84OUdoOWlycWR6N3hnZ3pHWloKMHUxeWFUdVlOUkVIMjA3OHoyS2htN0RMRTNwTysxVWR3K1BSalE9PTwvU2lnbmF0dXJlVmFsdWU+PEtleUluZm8+PFg1MDlEYXRhPjxYNTA5Q2VydGlmaWNhdGU+TUlJR3dqQ0NCYXFnQXdJQkFnSVRhZ0FBSExhR1g1UFpIVEZ2YkFBQkFBQWN0akFOQmdrcWhraUc5dzBCQVFzRkFEQlFNUlV3RXdZSwpDWkltaVpQeUxHUUJHUllGYkc5allXd3hGekFWQmdvSmtpYUprL0lzWkFFWkZnZHdjbVZ3Y205a01SNHdIQVlEVlFRREV4VkNNamNnClNYTnpkV2x1WnlCRFFTQkpiblJsY200d0hoY05NVGd4TURBME1URTFPRFUyV2hjTk1qQXhNREEwTVRJd09EVTJXakIrTVJVd0V3WUsKQ1pJbWlaUHlMR1FCR1JZRmJHOWpZV3d4RnpBVkJnb0praWFKay9Jc1pBRVpGZ2R3Y21Wd2NtOWtNUmd3RmdZRFZRUUxFdzlUWlhKMgphV05sUVdOamIzVnVkSE14RlRBVEJnTlZCQXNUREVGd2NHeEJZMk52ZFc1MGN6RWJNQmtHQTFVRUF4TVNjM0oyYzJWamRYSnBkSGt0CmRHOXJaVzR0TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFrZktVcUZVbjZDWnpsUkpRNVlLOFhGU1gKTHV6amVGWUN2eTdiOWVqeUE5eUVJdmNXalZKMnViYVpQNmhzbWxmdGRVUlBTOTBadFhLajIvL05UYjZGK2FZYjdWQ3JXb2N2MFNDZAo5WDMrYkh5S09lTEQ1b3ovd2RhaWdyQmZ4eGVUajF5WHNBZ3UzdHF5VDhJNnkyaWlCQjV6ZzlmS2xjazRnNko1cjdCTytiZG9wWUZlCkI1eDJ5bnZFY2pwM0d3OERHMlA5SCtSTlpycDVFdzJqdHY4aW0zV01Dc28zL1FiL0U3THN2Y1dDbmpXTkxRSElrTUh6OXJhb013ZGYKcnBzbnRSN1NaZDVFUllsdi96enBXSjF5MjNmRFljTlF1empTOW13Z2ZWYU9lZlpFRnlSNE9rdUpZK1lmaFoydWZxc0dmdmd1L3NMOApQVFBjL082RUdWTVNBUUlEQVFBQm80SURaVENDQTJFd0hRWURWUjBPQkJZRUZCUklTRE91cXZSRGY1ZS9GNXRma0s4c1BaWnpNQjhHCkExVWRJd1FZTUJhQUZPTm9ZMVc5MjJqYk56WGtZS2xTQjZzZ21xdU5NSUlCSVFZRFZSMGZCSUlCR0RDQ0FSUXdnZ0VRb0lJQkRLQ0MKQVFpR2djZHNaR0Z3T2k4dkwyTnVQVUl5TnlVeU1FbHpjM1ZwYm1jbE1qQkRRU1V5TUVsdWRHVnliaXhEVGoxQ01qZEVVbFpYTURBNApMRU5PUFVORVVDeERUajFRZFdKc2FXTWxNakJyWlhrbE1qQlRaWEoyYVdObGN5eERUajFUWlhKMmFXTmxjeXhEVGoxRGIyNW1hV2QxCmNtRjBhVzl1TEVSRFBYQnlaWEJ5YjJRc1JFTTliRzlqWVd3L1kyVnlkR2xtYVdOaGRHVlNaWFp2WTJGMGFXOXVUR2x6ZEQ5aVlYTmwKUDI5aWFtVmpkRU5zWVhOelBXTlNURVJwYzNSeWFXSjFkR2x2YmxCdmFXNTBoanhvZEhSd09pOHZZM0pzTG5CeVpYQnliMlF1Ykc5agpZV3d2UTNKc0wwSXlOeVV5TUVsemMzVnBibWNsTWpCRFFTVXlNRWx1ZEdWeWJpNWpjbXd3Z2dGakJnZ3JCZ0VGQlFjQkFRU0NBVlV3CmdnRlJNSUc4QmdnckJnRUZCUWN3QW9hQnIyeGtZWEE2THk4dlkyNDlRakkzSlRJd1NYTnpkV2x1WnlVeU1FTkJKVEl3U1c1MFpYSnUKTEVOT1BVRkpRU3hEVGoxUWRXSnNhV01sTWpCclpYa2xNakJUWlhKMmFXTmxjeXhEVGoxVFpYSjJhV05sY3l4RFRqMURiMjVtYVdkMQpjbUYwYVc5dUxFUkRQWEJ5WlhCeWIyUXNSRU05Ykc5allXdy9ZMEZEWlhKMGFXWnBZMkYwWlQ5aVlYTmxQMjlpYW1WamRFTnNZWE56ClBXTmxjblJwWm1sallYUnBiMjVCZFhSb2IzSnBkSGt3S2dZSUt3WUJCUVVITUFHR0htaDBkSEE2THk5dlkzTndMbkJ5WlhCeWIyUXUKYkc5allXd3ZiMk56Y0RCa0JnZ3JCZ0VGQlFjd0FvWllhSFIwY0RvdkwyTnliQzV3Y21Wd2NtOWtMbXh2WTJGc0wwTnliQzlDTWpkRQpVbFpYTURBNExuQnlaWEJ5YjJRdWJHOWpZV3hmUWpJM0pUSXdTWE56ZFdsdVp5VXlNRU5CSlRJd1NXNTBaWEp1S0RFcExtTnlkREFPCkJnTlZIUThCQWY4RUJBTUNCYUF3T3dZSkt3WUJCQUdDTnhVSEJDNHdMQVlrS3dZQkJBR0NOeFVJZ2RiVlhJT0FwMXlFOVowa202UlQKb0xKNWdTU0h2cDFGa1lNaUFnRmtBZ0VDTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjREFqQW5CZ2tyQmdFRQpBWUkzRlFvRUdqQVlNQW9HQ0NzR0FRVUZCd01CTUFvR0NDc0dBUVVGQndNQ01BMEdDU3FHU0liM0RRRUJDd1VBQTRJQkFRQ2xNQjhVCnZiQ1lLRlBJWFRPSzd4SmdKVDJPNUtGWkFMKytURGZaTVA4elF6VkQvcjBvZktrNmlGSzArdElrU0RlSUdHMy9OWk1Dd0dOMDRySE4KTWlpK2hwUThkRDl0Ym85eGtoVC9waElNVFU2YnVZc1BMcjltYWx4Wi9PWjZkcHdzclU4US9YM2NTekwyMEphZFY2TVVDaDY4Tit0RApTQzVjZXhPM2MwVFVUdGdhN0pJNGp0b0hQTms4K1FxVTREbXpJbVRtSlpjUGJJOU41bUVJNHFqaVN4Uk5KRkp3dzhHRkdlb1dRYklaClJNRkxvbmRsVTdNV1FwL2R2eFVzU0V3Nk9DQnFDSytOWFdUK1VnYUdUbGlRRVN0UGc5aElMT0VQOXlmKy9HVmpqalZLOEd2Mmg5aVIKYlNDZ0dxVWxTMVZZNnVQc3YwVUZXR0RpMGdzM3YrZUo8L1g1MDlDZXJ0aWZpY2F0ZT48WDUwOUlzc3VlclNlcmlhbD48WDUwOUlzc3Vlck5hbWU+Q049QjI3IElzc3VpbmcgQ0EgSW50ZXJuLCBEQz1wcmVwcm9kLCBEQz1sb2NhbDwvWDUwOUlzc3Vlck5hbWU+PFg1MDlTZXJpYWxOdW1iZXI+MjM2Mzg3OTAyOTIxMDM1MzM3ODU1Mjg2ODU5MjY5MDEwOTM2MjY3MjI0NTk0MjwvWDUwOVNlcmlhbE51bWJlcj48L1g1MDlJc3N1ZXJTZXJpYWw+PC9YNTA5RGF0YT48L0tleUluZm8+PC9TaWduYXR1cmU+PHNhbWwyOlN1YmplY3Q+PHNhbWwyOk5hbWVJRCBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjEuMTpuYW1laWQtZm9ybWF0OnVuc3BlY2lmaWVkIj5zcnZzcGlubmU8L3NhbWwyOk5hbWVJRD48c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpjbTpiZWFyZXIiPjxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9uRGF0YSBOb3RCZWZvcmU9IjIwMTgtMTEtMjhUMDk6NTM6MDIuMzIyWiIgTm90T25PckFmdGVyPSIyMDE4LTExLTI4VDEwOjUzOjAyLjMyMloiLz48L3NhbWwyOlN1YmplY3RDb25maXJtYXRpb24+PC9zYW1sMjpTdWJqZWN0PjxzYW1sMjpDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAxOC0xMS0yOFQwOTo1MzowMi4zMjJaIiBOb3RPbk9yQWZ0ZXI9IjIwMTgtMTEtMjhUMTA6NTM6MDIuMzIyWiIvPjxzYW1sMjpBdHRyaWJ1dGVTdGF0ZW1lbnQ+PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJpZGVudFR5cGUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dXJpIj48c2FtbDI6QXR0cmlidXRlVmFsdWU+U3lzdGVtcmVzc3Vyczwvc2FtbDI6QXR0cmlidXRlVmFsdWU+PC9zYW1sMjpBdHRyaWJ1dGU+PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJhdXRoZW50aWNhdGlvbkxldmVsIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OnVyaSI+PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPjA8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjxzYW1sMjpBdHRyaWJ1dGUgTmFtZT0iY29uc3VtZXJJZCIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYWx1ZT5zcnZzcGlubmU8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjwvc2FtbDI6QXR0cmlidXRlU3RhdGVtZW50Pjwvc2FtbDI6QXNzZXJ0aW9u",
  "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
  "token_type": "Bearer",
  "expires_in": 1
}"""

private val bad_saml_token = """{
  "access_token": "not base64 encoded",
  "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
  "token_type": "Bearer",
  "expires_in": 3599
}"""
