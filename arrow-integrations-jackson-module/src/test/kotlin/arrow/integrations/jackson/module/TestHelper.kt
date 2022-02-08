package arrow.integrations.jackson.module

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.kotest.matchers.shouldBe


inline fun <reified T> T.shouldRoundTrip(mapper: ObjectMapper) {
  val encoded = mapper.writeValueAsString(this)
  val decoded = mapper.readValue(encoded, jacksonTypeRef<T>())
  decoded shouldBe this
}

inline fun <reified T> String.shouldRoundTripOtherWay(mapper: ObjectMapper) {
  val decoded = mapper.readValue(this, jacksonTypeRef<T>())
  val encoded = mapper.writeValueAsString(decoded)
  mapper.readTree(encoded) shouldBe mapper.readTree(this)
}
