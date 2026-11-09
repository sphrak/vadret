package se.vadretapp.vadret.main

import se.vadretapp.vadret.main.navigation.VadretKey

class NativeRoute internal constructor(
    internal val key: VadretKey,
    val title: String,
    val presentation: NativePresentation
) {

    override fun equals(other: Any?): Boolean = other is NativeRoute && other.key == key

    override fun hashCode(): Int = key.hashCode()
}