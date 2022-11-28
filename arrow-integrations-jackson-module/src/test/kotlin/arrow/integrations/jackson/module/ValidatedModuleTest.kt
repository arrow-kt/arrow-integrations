package arrow.integrations.jackson.module

import arrow.core.Option
import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.arrow.core.option
import io.kotest.property.arrow.core.validated
import io.kotest.property.checkAll

class ValidatedModuleTest : FunSpec() {
  init {
    context("json serialization / deserialization") {
      test("should round-trip") { checkAll(arbTestClass) { it.shouldRoundTrip(mapper) } }

      test("should round-trip nullable types") {
        checkAll(Arb.validated(arbFoo.orNull(), arbBar.orNull())) { validated: Validated<Foo?, Bar?>
          ->
          validated.shouldRoundTrip(mapper)
        }
      }

      test("should round-trip other way") {
        checkAll(arbTestClassJsonString) { it.shouldRoundTripOtherWay<TestClass>(mapper) }
      }

      test("should round-trip nested validated types") {
        checkAll(Arb.validated(Arb.int(), Arb.validated(Arb.int(), Arb.string()).orNull())) {
          validated: Validated<Int, Validated<Int, String>?> ->
          validated.shouldRoundTrip(mapper)
        }
      }

      test("should serialize with configurable invalid / valid field name") {
        checkAll(
          Arb.pair(Arb.string(10, Codepoint.az()), Arb.string(10, Codepoint.az())).filter {
            it.first != it.second
          }
        ) { (invalidFieldName, validFieldName) ->
          val mapper =
            ObjectMapper()
              .registerKotlinModule()
              .registerArrowModule(
                validatedModuleConfig = ValidatedModuleConfig(invalidFieldName, validFieldName)
              )

          mapper.writeValueAsString(5.invalid()) shouldBe """{"$invalidFieldName":5}"""
          mapper.writeValueAsString("hello".valid()) shouldBe """{"$validFieldName":"hello"}"""
        }
      }

      test("should round-trip with configurable invalid / valid field name") {
        checkAll(
          Arb.pair(Arb.string(10, Codepoint.az()), Arb.string(10, Codepoint.az())).filter {
            it.first != it.second
          },
          arbTestClass
        ) { (invalidFieldName, validFieldName), testClass ->
          val mapper =
            ObjectMapper()
              .registerKotlinModule()
              .registerArrowModule(EitherModuleConfig(invalidFieldName, validFieldName))

          testClass.shouldRoundTrip(mapper)
        }
      }

      test("should round-trip with wildcard types") {
        checkAll(Arb.validated(Arb.int(1..10), Arb.string(10, Codepoint.az()))) {
          original: Validated<*, *> ->
          val mapper = ObjectMapper().registerKotlinModule().registerArrowModule()
          val serialized = mapper.writeValueAsString(original)
          val deserialized: Validated<*, *> = shouldNotThrowAny {
            mapper.readValue(serialized, Validated::class.java)
          }
          deserialized shouldBe original
        }
      }
    }
  }

  private val arbTestClassJsonString = arbitrary {
    if (Arb.boolean().bind()) {
      val foo = arbFoo.bind()
      """
        {
          "validated": {
            "invalid": {
              "foo": ${foo.fooValue.orNull()},
              "otherValue": ${mapper.writeValueAsString(foo.otherValue)}
            }
          }
        }
      """.trimIndent(
      )
    } else {
      val bar = arbBar.bind()
      """
        {
          "validated": {
            "valid": {
              "first": ${bar.first},
              "second": "${bar.second}",
              "third": ${bar.third}
            }
          }
        }
      """.trimIndent(
      )
    }
  }

  private data class Foo(
    @get:JsonProperty("foo") val fooValue: Option<Int>,
    val otherValue: String
  )
  private data class Bar(val first: Int, val second: String, val third: Boolean)
  private data class TestClass(val validated: Validated<Foo, Bar>)

  private val arbFoo: Arb<Foo> = arbitrary {
    Foo(Arb.option(Arb.int()).bind(), Arb.string().bind())
  }

  private val arbBar: Arb<Bar> = arbitrary {
    Bar(Arb.int().bind(), Arb.string(0..100, Codepoint.alphanumeric()).bind(), Arb.boolean().bind())
  }

  private val arbTestClass: Arb<TestClass> = arbitrary {
    TestClass(Arb.validated(arbFoo, arbBar).bind())
  }

  private val mapper = ObjectMapper().registerKotlinModule().registerArrowModule()
}
