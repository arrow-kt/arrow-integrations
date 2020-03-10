package arrow.integrations.retrofit.adapter

import arrow.core.test.UnitSpec
import arrow.fx.IO
import arrow.integrations.retrofit.adapter.retrofit.retrofit
import com.google.gson.reflect.TypeToken
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import okhttp3.HttpUrl

private val NO_ANNOTATIONS = emptyArray<Annotation>()

private val retrofit = retrofit(HttpUrl.parse("http://localhost:1")!!)
private val factory = CallKindAdapterFactory.create()

class CallKindAdapterFactoryTest : UnitSpec() {
  init {
    "Non CallK Class should return null" {
      factory.get(object : TypeToken<List<String>>() {}.type, NO_ANNOTATIONS, retrofit) shouldBe null
    }

    "Non parametrized type should throw exception" {
      val exceptionList = shouldThrow<IllegalArgumentException> {
        factory.get(List::class.java, NO_ANNOTATIONS, retrofit)
      }
      exceptionList.message shouldBe "Return type must be parameterized as List<Foo> or List<out Foo>"

      val exceptionIO = shouldThrow<IllegalArgumentException> {
        factory.get(IO::class.java, NO_ANNOTATIONS, retrofit)
      }
      exceptionIO.message shouldBe "Return type must be parameterized as IO<Foo> or IO<out Foo>"
    }

    "Should work for CallK types" {
      factory.get(object : TypeToken<CallK<String>>() {}.type, NO_ANNOTATIONS, retrofit)!!
        .responseType() shouldBe String::class.java
    }
  }
}
