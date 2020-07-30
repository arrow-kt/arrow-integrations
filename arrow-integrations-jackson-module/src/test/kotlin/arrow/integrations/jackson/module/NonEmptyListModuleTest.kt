package arrow.integrations.jackson.module

import arrow.core.Nel
import arrow.core.test.UnitSpec
import arrow.core.test.generators.nonEmptyList
import arrow.syntax.function.pipe
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.property.Arb
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class NonEmptyListModuleTest : UnitSpec() {
  private val mapper = ObjectMapper().registerModule(NonEmptyListModule).registerKotlinModule()

  init {
    "serializing NonEmptyList should be the same as serializing the underlying list" {
      checkAll(Arb.nonEmptyList(Arb.choice(Arb.someObject(), Arb.int(), Arb.string(), Arb.bool()))) { list ->
        val actual = mapper.writeValueAsString(list)
        val expected = mapper.writeValueAsString(list.all)

        actual shouldBe expected
      }
    }

    "serializing NonEmptyList and then deserialize it should be the same as before the deserialization" {
      checkAll(
        Arb.choice(
          Arb.nonEmptyList(Arb.someObject()).map { it to jacksonTypeRef<Nel<SomeObject>>() },
          Arb.nonEmptyList(Arb.int()).map { it to jacksonTypeRef<Nel<Int>>() },
          Arb.nonEmptyList(Arb.string()).map { it to jacksonTypeRef<Nel<String>>() },
          Arb.nonEmptyList(Arb.bool()).map { it to jacksonTypeRef<Nel<Boolean>>() }
        )
      ) { (list, typeReference) ->
        val roundTripped = mapper.writeValueAsString(list).pipe { mapper.readValue<Nel<*>>(it, typeReference) }

        roundTripped shouldBe list
      }
    }
  }
}
