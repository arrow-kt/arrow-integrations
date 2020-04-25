package arrow.integrations.retrofit.adapter

import arrow.core.Either
import okhttp3.Headers

data class ResponseE<E, A>(
  val raw: okhttp3.Response,
  val body: Either<E, A>
) {

  val code: Int = raw.code()

  val message: String? = raw.message()

  val headers: Headers = raw.headers()

  val isSuccessful: Boolean = raw.isSuccessful
}
