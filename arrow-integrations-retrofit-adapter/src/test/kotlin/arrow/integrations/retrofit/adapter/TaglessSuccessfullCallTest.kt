package arrow.integrations.retrofit.adapter

import arrow.core.right
import arrow.core.test.UnitSpec
import arrow.fx.reactor.MonoK
import arrow.fx.reactor.extensions.monok.async.async
import arrow.fx.reactor.unsafeRunSync
import arrow.fx.rx2.ObservableK
import arrow.fx.rx2.extensions.observablek.async.async
import arrow.integrations.retrofit.adapter.io.TaglessAdapterFactory
import arrow.integrations.retrofit.adapter.mock.ResponseMock
import arrow.integrations.retrofit.adapter.retrofit.IOApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.MonoKApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.ObservableKApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.retrofit
import io.kotlintest.fail
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals

class TaglessSuccessfullCallTest : UnitSpec() {

  private fun server(): MockWebServer = MockWebServer().apply {
    enqueue(MockResponse().setBody("{\"response\":  \"hello, world!\"}").setResponseCode(200))
    start()
  }

  private fun baseUrl(): HttpUrl = server().url("/")

  init {
    "should be able to parse answer with IO" {
      createIOApiClientTest(baseUrl())
        .testIO()
        .unsafeRunSyncEither()
        .fold({
          fail("The request terminated with an error (IO left value)")
        }, { responseMock ->
          assertEquals(ResponseMock("hello, world!"), responseMock)
        })
    }

    "should be able to parse answer with ObservableK" {
      createObservableKApiClientTest(baseUrl())
        .testObservableK()
        .observable
        .test()
        .await()
        .assertValue { it == ResponseMock("hello, world!") }
    }

    "should be able to parse answer with MonoK" {
      val result = createMonoKApiClientTest(baseUrl())
        .testMonoK()
        .unsafeRunSync()
      assertEquals(result, ResponseMock("hello, world!"))
    }

    "should be able to run IO POST with UNIT as response" {
      val result = createIOApiClientTest(baseUrl())
        .testUnitResponsePost()
        .unsafeRunSyncEither()
      assertEquals(result, Unit.right())
    }

    "should be able to run ObservableK POST with UNIT as response" {
      createObservableKApiClientTest(baseUrl())
        .testUnitResponsePost()
        .observable
        .test()
        .await()
        .assertValue { it == Unit }
    }

    "should be able to run MonoK POST with UNIT as response" {
      val result = createMonoKApiClientTest(baseUrl())
        .testUnitResponsePost()
        .unsafeRunSync()

      assertEquals(result, Unit)
    }
  }
}

private fun createIOApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.createIO()).create(IOApiClientTest::class.java)

private fun createObservableKApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.create(ObservableK.async())).create(ObservableKApiClientTest::class.java)

private fun createMonoKApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.create(MonoK.async())).create(MonoKApiClientTest::class.java)
