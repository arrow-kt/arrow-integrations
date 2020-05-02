package arrow.integrations.retrofit.adapter.retrofit

import arrow.integrations.retrofit.adapter.callk.CallK
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import retrofit2.http.GET
import retrofit2.http.POST

interface CallKApiClientTest {

  @GET("test")
  fun testCallK(): CallK<ResponseMock>

  @POST("testIOResponsePOST")
  fun testIOResponsePost(): CallK<Unit>
}
