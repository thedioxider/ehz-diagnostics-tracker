package space.diomentia.ehzdiagnosticstracker.data

import java.security.MessageDigest

class PasswordHash private constructor(val hash: String) {
    companion object {
        const val SPECIAL_SYMBOLS = "!@#$%^&*()-=+/,.\"';:?`~[]{}"

        fun checkInput(password: String): Boolean = Regex(
            "^[\\w0-9!@#$%^&*()\\-=+/,.\"';:?`~\\[\\]{}]+$"
        ).matches(password)

        fun encrypt(password: String): PasswordHash? {
            if (!checkInput(password)) return null
            return PasswordHash(
                MessageDigest.getInstance("SHA-512")
                    .digest(password.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            )
        }

        fun fromHash(hash: String): PasswordHash {
            if (!Regex("^[0-9a-fA-F]{128}$").matches(hash))
                throw IllegalArgumentException("not a hash string")
            return PasswordHash(hash)
        }
    }

    init {
        if (!hash.matches(Regex("^[0-9a-fA-F]+$"))) {
            throw IllegalArgumentException("password hash is not a hexadecimal")
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    override operator fun equals(other: Any?): Boolean {
        return when (other) {
            is PasswordHash -> hash == other.hash
            else -> super.equals(other)
        }
    }
}
