package arrow.integrations.jackson.module

import arrow.core.Option
import arrow.core.test.UnitSpec
import arrow.core.test.generators.option
import arrow.syntax.function.pipe
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.property.Arb
import io.kotest.matchers.shouldBe
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class OptionModuleTest : UnitSpec() {
  private val mapper = ObjectMapper().registerModule(OptionModule).registerKotlinModule()

  init {
    "serializing Option should be the same as serializing a nullable value" {
      checkAll(Arb.option(Arb.choice(Arb.someObject(), Arb.int(), Arb.string(), Arb.bool()))) { option ->
        val actual = mapper.writeValueAsString(option)
        val expected = mapper.writeValueAsString(option.orNull())

        actual shouldBe expected
      }
    }

    "serializing Option and then deserialize it should be the same as before the deserialization" {
      checkAll(
        Arb.choice(
          Arb.option(Arb.someObject()).map { it to jacksonTypeRef<Option<SomeObject>>() },
          Arb.option(Arb.int()).map { it to jacksonTypeRef<Option<Int>>() },
          Arb.option(Arb.string()).map { it to jacksonTypeRef<Option<String>>() },
          Arb.option(Arb.bool()).map { it to jacksonTypeRef<Option<Boolean>>() }
        )
      ) { (option, typeReference) ->
        val roundTripped = mapper.writeValueAsString(option).pipe { mapper.readValue<Option<*>>(it, typeReference) }

        roundTripped shouldBe option
      }
    }
  }
}
