package arrow.integrations.retrofit.adapter.retrofit

import arrow.fx.reactor.MonoK
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import retrofit2.http.GET
import retrofit2.http.POST

interface MonoKApiClientTest {

  @GET("test")
  fun testMonoK(): MonoK<ResponseMock>

  @POST("testUnitResponsePOST")
  fun testUnitResponsePost(): MonoK<Unit>
}
