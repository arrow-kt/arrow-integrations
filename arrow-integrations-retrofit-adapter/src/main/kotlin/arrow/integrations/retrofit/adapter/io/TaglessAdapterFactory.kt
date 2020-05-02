package arrow.integrations.retrofit.adapter.io

import arrow.Kind
import arrow.fx.IO
import arrow.fx.IOPartialOf
import arrow.fx.extensions.io.async.async
import arrow.fx.typeclasses.Async
import arrow.integrations.retrofit.adapter.parseTypeName
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class TaglessAdapterFactory<F>(private val async: Async<F>) : CallAdapter.Factory() {

  companion object {
    fun createIO(): TaglessAdapterFactory<IOPartialOf<Throwable>> =
      TaglessAdapterFactory(IO.async())

    fun <F> create(async: Async<F>): TaglessAdapterFactory<F> =
      TaglessAdapterFactory(async)
  }

  override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
    val rawType = getRawType(returnType)

    if (returnType !is ParameterizedType) {
      val name = parseTypeName(returnType)
      throw IllegalArgumentException("Return type must be parameterized as " +
        "$name<Foo> or $name<out Foo>")
    }

    return if (Kind::class.java.isAssignableFrom(rawType)) {
      val effectType = getParameterUpperBound(returnType.actualTypeArguments.lastIndex, returnType)
      TaglessCallAdapter<F, Type>(effectType, async)
    } else {
      null
    }
  }

}
