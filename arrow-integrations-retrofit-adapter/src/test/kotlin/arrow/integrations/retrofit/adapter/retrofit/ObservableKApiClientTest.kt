package arrow.integrations.retrofit.adapter.retrofit

import arrow.fx.rx2.ObservableK
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import retrofit2.http.GET
import retrofit2.http.POST

interface ObservableKApiClientTest {

  @GET("test")
  fun testObservableK(): ObservableK<ResponseMock>

  @POST("testUnitResponsePOST")
  fun testUnitResponsePost(): ObservableK<Unit>
}
