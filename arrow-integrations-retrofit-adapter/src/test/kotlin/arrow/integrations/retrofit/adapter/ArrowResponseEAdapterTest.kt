import arrow.core.left
import arrow.core.right
import arrow.integrations.retrofit.adapter.mock.ErrorMock
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import arrow.integrations.retrofit.adapter.retrofit.SuspedApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.retrofit
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy

class ArrowResponseEAdapterTest : StringSpec() {

  private val server = MockWebServer()

  private val service: SuspedApiClientTest by lazy {
    retrofit(HttpUrl.get("/"))
      .create(SuspedApiClientTest::class.java)
  }

  override fun beforeSpec(spec: Spec) {
    super.beforeSpec(spec)
    server.start()
  }

  override fun afterSpec(spec: Spec) {
    server.shutdown()
    super.afterSpec(spec)
  }

  init {

    "adapts a successful response" {
      server.enqueue(MockResponse().setBody("""{"response":"Arrow rocks"}"""))

      val responseE = runBlocking { service.getResponseE() }

      with(responseE) {
        code shouldBe 200
        body shouldBe ResponseMock("Arrow rocks").right()
      }
    }

    "adapts an error call" {
      server.enqueue(MockResponse().setBody("""{"responseCode":42}""").setResponseCode(400))

      val responseE = runBlocking { service.getResponseE() }

      with(responseE) {
        code shouldBe 400
        body shouldBe ErrorMock(42).left()
      }
    }

    "call with timeout" {
      server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST })

      val body = runCatching { service.getResponseE() }

      body.isFailure shouldBe true
    }
  }
}
