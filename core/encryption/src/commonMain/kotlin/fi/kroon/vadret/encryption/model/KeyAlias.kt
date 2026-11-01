package fi.kroon.vadret.encryption.model

import kotlin.jvm.JvmInline

@JvmInline
value class KeyAlias(val value: String) {
    init {
        require(value = keyAliasPattern.matches(input = value)) { "A key alias is 1 to 128 letters, digits, dots, dashes or underscores, was: $value" }
    }
}

private val keyAliasPattern = Regex(pattern = "[A-Za-z0-9._-]{1,128}")