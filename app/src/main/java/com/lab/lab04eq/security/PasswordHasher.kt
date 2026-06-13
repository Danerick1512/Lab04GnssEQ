package com.lab.lab04eq.security

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val ALGORITMO           = "PBKDF2WithHmacSHA256"
    private const val ITERACIONES         = 120_000
    private const val LONGITUD_HASH_BITS  = 256

    /**
     * Genera un hash criptográfico robusto a partir de una contraseña y una semilla (salt).
     */
    fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERACIONES,
            LONGITUD_HASH_BITS
        )
        val factory = SecretKeyFactory.getInstance(ALGORITMO)
        val bytes   = factory.generateSecret(spec).encoded

        spec.clearPassword()  // Borrado defensivo de la contraseña en memoria

        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Compara dos hashes en un tiempo constante uniforme para evitar ataques de canal lateral (Timing Attacks).
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }
}