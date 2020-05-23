package arrow.integrations.retrofit.adapter.retrofit

import arrow.fx.IO
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import retrofit2.http.GET
import retrofit2.http.POST

interface IOApiClientTest {

  @GET("test")
  fun testIO(): IO<Nothing, ResponseMock>

  @POST("testUnitResponsePOST")
  fun testUnitResponsePost(): IO<Nothing, Unit>
}
