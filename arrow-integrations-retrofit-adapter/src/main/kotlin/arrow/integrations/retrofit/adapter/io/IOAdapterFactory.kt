package arrow.integrations.retrofit.adapter.io

import arrow.fx.IO
import arrow.integrations.retrofit.adapter.parseTypeName
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class IOAdapterFactory : CallAdapter.Factory() {

  companion object {
    fun create(): IOAdapterFactory =
      IOAdapterFactory()
  }

  override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
    val rawType = getRawType(returnType)

    if (returnType !is ParameterizedType) {
      val name = parseTypeName(returnType)
      throw IllegalArgumentException("Return type must be parameterized as " +
        "$name<Foo> or $name<out Foo>")
    }

    return if (rawType == IO::class.java) {
      val effectType = getParameterUpperBound(1, returnType)
      IOCallAdapter<Type>(effectType)
    } else {
      null
    }
  }

}
