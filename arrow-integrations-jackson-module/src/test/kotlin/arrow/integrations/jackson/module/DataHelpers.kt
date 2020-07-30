package arrow.integrations.jackson.module

import io.kotest.property.Arb

data class SomeObject(val someString: String, val someInt: Int)

fun Arb.Companion.someObject(): Arb<SomeObject> = bind(string(), int()) { str, int -> SomeObject(str, int) }
