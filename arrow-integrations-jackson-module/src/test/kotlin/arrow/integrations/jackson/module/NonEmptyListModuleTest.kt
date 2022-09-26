package arrow.integrations.jackson.module

import arrow.core.Nel
import arrow.core.NonEmptyList
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.arrow.core.nonEmptyList
import io.kotest.property.checkAll

class NonEmptyListModuleTest : FunSpec() {
  private val mapper = ObjectMapper().registerModule(NonEmptyListModule).registerKotlinModule()

  init {
    test("serializing NonEmptyList should be the same as serializing the underlying list") {
      checkAll(
        Arb.nonEmptyList(Arb.choice(Arb.someObject(), Arb.int(), Arb.string(), Arb.boolean()))
      ) { list ->
        val actual = mapper.writeValueAsString(list)
        val expected = mapper.writeValueAsString(list.all)

        actual shouldBe expected
      }
    }

    test(
      "serializing NonEmptyList and then deserialize it should be the same as before the deserialization"
    ) {
      checkAll(
        Arb.choice(
          arbitrary {
            Arb.nonEmptyList(Arb.someObject()).bind() to jacksonTypeRef<Nel<SomeObject>>()
          },
          arbitrary { Arb.nonEmptyList(Arb.int()).bind() to jacksonTypeRef<Nel<Int>>() },
          arbitrary { Arb.nonEmptyList(Arb.string()).bind() to jacksonTypeRef<Nel<String>>() },
          arbitrary { Arb.nonEmptyList(Arb.boolean()).bind() to jacksonTypeRef<Nel<Boolean>>() }
        )
      ) { (list, typeReference) ->
        val encoded: String = mapper.writeValueAsString(list)
        val decoded: Nel<Any> = mapper.readValue(encoded, typeReference)

        decoded shouldBe list
      }
    }

    test("serializing NonEmptyList in an object should round trip") {
      data class Wrapper(val nel: Nel<SomeObject>)
      checkAll(arbitrary { Wrapper(Arb.nonEmptyList(Arb.someObject()).bind()) }) { wrapper ->
        val encoded: String = mapper.writeValueAsString(wrapper)
        val decoded: Wrapper = mapper.readValue(encoded, Wrapper::class.java)

        decoded shouldBe wrapper
      }
    }

    test("should round trip on NonEmptyList with wildcard type") {
      val mapperWithSettings = ObjectMapper().registerKotlinModule().registerArrowModule()

      checkAll(Arb.nonEmptyList(Arb.string())) { original: NonEmptyList<*> ->
        val deserialized: NonEmptyList<*> = shouldNotThrowAny {
          val serialized: String = mapperWithSettings.writeValueAsString(original)
          mapperWithSettings.readValue(serialized, NonEmptyList::class.java)
        }

        deserialized shouldBe original
      }
    }
  }
}
