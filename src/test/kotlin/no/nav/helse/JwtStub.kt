package no.nav.helse

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.io.core.String
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

class JwtStub(private val issuer: String) {

    private val privateKey: RSAPrivateKey
    private val publicKey: RSAPublicKey

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(512)

        val keyPair = keyPairGenerator.genKeyPair()
        privateKey = keyPair.private as RSAPrivateKey
        publicKey = keyPair.public as RSAPublicKey
    }

    fun createTokenFor(subject: String): String {
        val algorithm = Algorithm.RSA256(publicKey, privateKey)

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .sign(algorithm)
    }

    fun stubbedJwkProvider(): StubbedJwkProvider {
        return StubbedJwkProvider(publicKey)
    }

    class StubbedJwkProvider(private val publicKey: RSAPublicKey) : JwkProvider {
        override fun get(keyId: String?): Jwk {
            return Jwk(keyId, "RSA", "RS256", "sig", listOf(), null, null, null, mapOf(
                    "e" to String(Base64.getEncoder().encode(publicKey.publicExponent.toByteArray())),
                    "n" to String(Base64.getEncoder().encode(publicKey.modulus.toByteArray()))
            ))
        }

    }
}
