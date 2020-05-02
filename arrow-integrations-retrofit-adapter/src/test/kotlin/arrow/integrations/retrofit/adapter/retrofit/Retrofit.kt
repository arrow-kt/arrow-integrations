package arrow.integrations.retrofit.adapter.retrofit

import arrow.integrations.retrofit.adapter.callk.CallKindAdapterFactory
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private fun provideOkHttpClient(): OkHttpClient =
  OkHttpClient.Builder().build()

private fun Retrofit.Builder.configure(adapterFactory: CallAdapter.Factory) =
  this
    .addCallAdapterFactory(adapterFactory)
    .addConverterFactory(GsonConverterFactory.create())
    .client(provideOkHttpClient())

private fun getRetrofitBuilderDefaults(baseUrl: HttpUrl) = Retrofit.Builder().baseUrl(baseUrl)

fun retrofit(baseUrl: HttpUrl, adapterFactory: CallAdapter.Factory = CallKindAdapterFactory.create()): Retrofit =
  getRetrofitBuilderDefaults(baseUrl).configure(adapterFactory).build()
