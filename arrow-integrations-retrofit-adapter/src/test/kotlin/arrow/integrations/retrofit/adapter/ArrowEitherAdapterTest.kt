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

class ArrowEitherAdapterTest : StringSpec() {
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

      val body = runBlocking { service.getEither() }

      body shouldBe ResponseMock("Arrow rocks").right()
    }

    "adapts an error call" {
      server.enqueue(MockResponse().setBody("""{"errorCode":666}""").setResponseCode(400))

      val body = runBlocking { service.getEither() }

      body shouldBe ErrorMock(666).left()
    }

    "call with timeout" {
      server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST })

      val body = runCatching { service.getEither() }

      body.isFailure shouldBe true
    }
  }
}
