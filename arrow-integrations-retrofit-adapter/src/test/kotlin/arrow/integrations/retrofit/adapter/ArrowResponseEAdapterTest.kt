package arrow.integrations.retrofit.adapter

import arrow.core.left
import arrow.core.right
import arrow.core.test.UnitSpec
import arrow.integrations.retrofit.adapter.mock.ErrorMock
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import arrow.integrations.retrofit.adapter.retrofit.SuspedApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.retrofit
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy

class ArrowResponseEAdapterTest : UnitSpec() {

  private val server = MockWebServer()

  private val service: SuspedApiClientTest by lazy {
    retrofit(server.url("/"))
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

    "should return ResponseMock for 200 with valid JSON" {
      server.enqueue(MockResponse().setBody("""{"response":"Arrow rocks"}"""))

      val responseE = runBlocking { service.getResponseE() }

      with(responseE) {
        code shouldBe 200
        body shouldBe ResponseMock("Arrow rocks").right()
      }
    }

    "should return ErrorMock for 400 with valid JSON" {
      server.enqueue(MockResponse().setBody("""{"errorCode":42}""").setResponseCode(400))

      val responseE = runBlocking { service.getResponseE() }

      with(responseE) {
        code shouldBe 400
        body shouldBe ErrorMock(42).left()
      }
    }

    "should throw for 200 with invalid JSON" {
      server.enqueue(MockResponse().setBody("""not a valid JSON"""))

      val responseE = kotlin.runCatching { service.getResponseE() }

      responseE.isFailure shouldBe true
    }

    "should throw for 400 and invalid JSON" {
      server.enqueue(MockResponse().setBody("""not a valid JSON""").setResponseCode(400))

      val responseE = kotlin.runCatching { service.getResponseE() }

      responseE.isFailure shouldBe true
    }

    "should throw when server disconnects" {
      server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST })

      val responseE = runCatching { service.getResponseE() }

      responseE.isFailure shouldBe true
    }
  }
}
