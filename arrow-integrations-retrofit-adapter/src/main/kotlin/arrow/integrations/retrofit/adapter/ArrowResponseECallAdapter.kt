package arrow.integrations.retrofit.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import retrofit2.*
import java.lang.reflect.Type

internal class ArrowResponseECallAdapter<E, R>(
  retrofit: Retrofit,
  errorType: Type,
  private val bodyType: Type
) : CallAdapter<R, Call<ResponseE<E, R>>> {

  private val errorConverter: Converter<ResponseBody, E> =
    retrofit.responseBodyConverter(errorType, arrayOfNulls(0))

  override fun adapt(call: Call<R>): Call<ResponseE<E, R>> = ResponseECall(call, errorConverter)

  override fun responseType(): Type = bodyType

  class ResponseECall<E, R>(
    private val original: Call<R>,
    private val errorConverter: Converter<ResponseBody, E>
  ) : Call<ResponseE<E, R>> {

    override fun enqueue(callback: Callback<ResponseE<E, R>>) {
      original.enqueue(object : Callback<R> {

        override fun onFailure(call: Call<R>, t: Throwable) {
          callback.onFailure(this@ResponseECall, t)
        }

        override fun onResponse(call: Call<R>, response: Response<R>) {
          if (response.isSuccessful) {
            val body = response.body()
            if (body == null) {
              callback.onFailure(this@ResponseECall, IllegalStateException("Null body found!"))
            } else {
              val bodyE: Either<E, R> = body.right()
              callback.onResponse(this@ResponseECall, Response.success(response.code(), ResponseE(response.raw(), bodyE)))
            }
          } else {
            val error = response.errorBody()
            if (error == null) {
              callback.onFailure(this@ResponseECall, IllegalStateException("Null errorBody found!"))
            } else {
              try {
                val errorBody = errorConverter.convert(response.errorBody()!!)
                if (errorBody == null) {
                  callback.onFailure(this@ResponseECall, IllegalStateException("Null errorBody found!"))
                } else {
                  callback.onResponse(this@ResponseECall, Response.success<ResponseE<E, R>>(ResponseE(response.raw(), errorBody.left())))
                }
              } catch (e: Exception) {
                callback.onFailure(this@ResponseECall, IllegalStateException("Failed to convert error body!", e))
              }
            }
          }
        }
      })
    }

    override fun isExecuted(): Boolean = original.isExecuted

    override fun timeout(): Timeout = original.timeout()

    override fun clone(): Call<ResponseE<E, R>> = ResponseECall(original.clone(), errorConverter)

    override fun isCanceled(): Boolean = original.isCanceled

    override fun cancel() = original.cancel()

    override fun execute(): Response<ResponseE<E, R>> =
      throw UnsupportedOperationException("We don't do that here!")

    override fun request(): Request = original.request()
  }
}
