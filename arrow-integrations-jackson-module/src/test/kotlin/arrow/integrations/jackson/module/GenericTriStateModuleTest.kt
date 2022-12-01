package arrow.integrations.jackson.module

import arrow.integrations.jackson.module.GenericTriStateModuleTest.TriState.Companion.defined
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class GenericTriStateModuleTest : FunSpec() {
  init {
    context("json serialization / deserialization") {
      test("should round-trip absent") {
        val serialized = mapper.writeValueAsString(Example(TriState.Absent))
        val deserialized = mapper.readValue(serialized, Example::class.java)

        serialized shouldBe "{}"
        deserialized shouldBe Example(TriState.absent())
      }

      test("should round-trip null") {
        val serialized = mapper.writeValueAsString(Example(TriState.nul()))
        val deserialized = mapper.readValue(serialized, Example::class.java)

        serialized shouldBe """{"nested":null}"""
        deserialized shouldBe Example(TriState.nul())
      }

      test("should round-trip defined") {
        checkAll(Arb.string(0..100, Codepoint.alphanumeric())) { value ->
          val serialized = mapper.writeValueAsString(Example(Nested(value).defined()))
          val deserialized = mapper.readValue(serialized, Example::class.java)

          serialized shouldBe """{"nested":{"value":"$value"}}"""
          deserialized shouldBe Example(Nested(value).defined())
        }
      }
    }
  }

  private sealed class TriState<out A> {
    companion object {
      fun <T> T.defined(): TriState<T> = Defined(this)
      fun <T> absent(): TriState<T> = Absent
      fun <T> nul(): TriState<T> = Null
    }

    object Absent : TriState<Nothing>()
    object Null : TriState<Nothing>()
    data class Defined<T>(val value: T) : TriState<T>()
  }

  private data class Nested(val value: String)

  private data class Example(val nested: TriState<Nested>)

  private val tristateModule: GenericTriStateModule<TriState<*>> =
    GenericTriStateModule(
      serializationConfig =
        GenericTriStateSerializationConfig(
          isPresent = {
            when (it) {
              TriState.Absent -> false
              is TriState.Defined -> true
              TriState.Null -> true
            }
          },
          serializeValue = {
            when (it) {
              TriState.Absent -> SerializedValue.AbsentOrNull
              is TriState.Defined -> SerializedValue.Value(it.value)
              TriState.Null -> SerializedValue.ExplicitNull
            }
          }
        ),
      deserializationConfig =
        GenericTriStateDeserializationConfig(
          ifAbsent = { TriState.Absent },
          ifNull = { TriState.Null },
          ifDefined = { TriState.Defined(it) }
        )
    )

  private val mapper =
    ObjectMapper()
      .registerKotlinModule()
      .registerModule(tristateModule)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
}
