package arrow.integrations.retrofit.adapter.io

import arrow.Kind
import arrow.core.left
import arrow.core.right
import arrow.fx.typeclasses.Async
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.lang.reflect.Type

class TaglessCallAdapter<F, R>(private val type: Type, private val async: Async<F>) : CallAdapter<R, Kind<F, R>> {
  override fun adapt(call: Call<R>): Kind<F, R> =
    async.async { ioProc ->
      call.enqueue(object : Callback<R> {
        override fun onResponse(call: Call<R>, response: Response<R>) {
          if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
              ioProc(body.right())
            } else {
              ioProc(IllegalStateException("The request returned a null body").left())
            }
          } else {
            ioProc(HttpException(response).left())
          }
        }

        override fun onFailure(call: Call<R>, throwable: Throwable) {
          ioProc(throwable.left())
        }
      })
    }

  override fun responseType(): Type = type
}
