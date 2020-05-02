package arrow.integrations.retrofit.adapter.io

import arrow.Kind
import arrow.core.Either
import arrow.core.ForEither
import arrow.core.extensions.either.applicativeError.applicativeError
import arrow.core.fix
import arrow.fx.typeclasses.Async
import arrow.typeclasses.ApplicativeError
import retrofit2.*
import java.lang.reflect.Type

class TaglessCallAdapter<F, R>(private val type: Type, private val async: Async<F>) : CallAdapter<R, Kind<F, R>> {
  override fun adapt(call: Call<R>): Kind<F, R> {
    val apError: ApplicativeError<Kind<ForEither, Throwable>, Throwable> = Either.applicativeError()

    return async.async { ioProc ->
      call.enqueue(object : Callback<R> {
        override fun onResponse(call: Call<R>, response: Response<R>) {
          if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
              ioProc(apError.just(body).fix())
            } else {
              ioProc(apError.raiseError<R>(IllegalStateException("The request returned a null body")).fix())
            }
          } else {
            ioProc(apError.raiseError<R>(HttpException(response)).fix())
          }
        }

        override fun onFailure(call: Call<R>, t: Throwable) {
          ioProc(apError.raiseError<R>(t).fix())
        }
      })
    }
  }

  override fun responseType(): Type = type
}
