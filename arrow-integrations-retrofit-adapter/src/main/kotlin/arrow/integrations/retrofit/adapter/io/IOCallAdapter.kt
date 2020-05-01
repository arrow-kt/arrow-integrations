package arrow.integrations.retrofit.adapter.io

import arrow.fx.IO
import arrow.fx.IOPartialOf
import arrow.fx.extensions.io.applicativeError.applicativeError
import arrow.fx.extensions.io.async.async
import arrow.fx.flatMap
import arrow.integrations.retrofit.adapter.runAsync
import arrow.integrations.retrofit.adapter.unwrapBody
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

class IOCallAdapter<R>(private val type: Type) : CallAdapter<R, IO<Throwable, R>> {
  override fun adapt(call: Call<R>): IO<Throwable, R> =
    call.runAsync(IO.async<Throwable>())
      .flatMap { it.unwrapBody<IOPartialOf<Throwable>, R>(IO.applicativeError()) }

  override fun responseType(): Type = type
}
