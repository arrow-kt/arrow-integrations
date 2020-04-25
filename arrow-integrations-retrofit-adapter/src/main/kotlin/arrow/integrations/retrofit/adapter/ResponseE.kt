package arrow.integrations.retrofit.adapter

import arrow.core.Either
import okhttp3.Headers

data class ResponseE<E, A>(
    val raw: okhttp3.Response,
    val code: Int,
    val message: String?,
    val headers: Headers,
    val body: Either<E, A>
) {

    val isSuccessful: Boolean = raw.isSuccessful
}
