package arrow.integrations.jackson.module

import arrow.core.Nel
import arrow.core.test.UnitSpec
import arrow.core.test.generators.nonEmptyList
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe

class NonEmptyListModuleTest : UnitSpec() {
  private val mapper = ObjectMapper().registerModule(NonEmptyListModule).registerKotlinModule()

  init {
    "serializing NonEmptyList should be the same as serializing the underlying list" {
      assertAll(Gen.nonEmptyList(Gen.oneOf(Gen.someObject(), Gen.int(), Gen.string(), Gen.bool()))) { list ->
        val actual = mapper.writeValueAsString(list)
        val expected = mapper.writeValueAsString(list.all)

        actual shouldBe (expected)
      }
    }

    "serializing NonEmptyList and then deserialize it should be the same as before the deserialization" {
      assertAll(
        Gen.oneOf(
          Gen.nonEmptyList(Gen.someObject()).map { it to jacksonTypeRef<Nel<SomeObject>>() },
          Gen.nonEmptyList(Gen.int()).map { it to jacksonTypeRef<Nel<Int>>() },
          Gen.nonEmptyList(Gen.string()).map { it to jacksonTypeRef<Nel<String>>() },
          Gen.nonEmptyList(Gen.bool()).map { it to jacksonTypeRef<Nel<Boolean>>() }
        )
      ) { (list, typeReference) ->
        val encoded: String = mapper.writeValueAsString(list)
        val decoded: Nel<Any> = mapper.readValue(encoded, typeReference)

        decoded shouldBe list
      }
    }

    "serializing NonEmptyList in an object should round trip" {
      data class Wrapper(val nel: Nel<SomeObject>)
      assertAll(Gen.nonEmptyList(Gen.someObject()).map { Wrapper(it) }) { wrapper ->
        val encoded: String = mapper.writeValueAsString(wrapper)
        val decoded: Wrapper = mapper.readValue(encoded, Wrapper::class.java)

        decoded shouldBe wrapper
      }
    }
  }
}
