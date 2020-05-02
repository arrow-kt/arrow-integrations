package arrow.integrations.retrofit.adapter

import arrow.Kind
import arrow.fx.typeclasses.Async
import arrow.fx.typeclasses.MonadDefer
import arrow.integrations.retrofit.adapter.callk.CallK
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.MonadError
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import java.lang.reflect.Type

fun <F, R> Response<R>.unwrapBody(apError: ApplicativeError<F, Throwable>): Kind<F, R> =
  if (this.isSuccessful) {
    val body = this.body()
    if (body != null) {
      apError.just(body)
    } else {
      apError.raiseError(IllegalStateException("The request returned a null body"))
    }
  } else {
    apError.raiseError(HttpException(this))
  }

fun <F, A> Call<A>.runAsync(AC: Async<F>): Kind<F, Response<A>> =
  AC.async { callback ->
    enqueue(ResponseCallback(callback))
  }

fun <F, A> Call<A>.runSyncDeferred(defer: MonadDefer<F>): Kind<F, Response<A>> = defer.later { execute() }

fun <F, A> Call<A>.runSyncCatch(monadError: MonadError<F, Throwable>): Kind<F, Response<A>> =
  monadError.run {
    catch {
      execute()
    }
  }

internal fun parseTypeName(type: Type) =
  type.toString()
    .split(".")
    .last()

@Deprecated("CallK moved", ReplaceWith("CallK", "arrow.integrations.retrofit.adapter.callk.CallK"))
typealias CallK<R> = arrow.integrations.retrofit.adapter.callk.CallK<R>
@Deprecated("CallKind2CallAdapter moved", ReplaceWith("CallKind2CallAdapter", "arrow.integrations.retrofit.adapter.callk.CallKind2CallAdapter"))
typealias CallKind2CallAdapter<R> = arrow.integrations.retrofit.adapter.callk.CallKind2CallAdapter<R>
@Deprecated("CallKindAdapterFactory moved", ReplaceWith("CallKindAdapterFactory", "arrow.integrations.retrofit.adapter.callk.CallKindAdapterFactory"))
typealias CallKindAdapterFactory = arrow.integrations.retrofit.adapter.callk.CallKindAdapterFactory
