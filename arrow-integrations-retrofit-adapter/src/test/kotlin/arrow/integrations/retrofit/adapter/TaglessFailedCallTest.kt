package arrow.integrations.retrofit.adapter

import arrow.core.Either
import arrow.core.test.UnitSpec
import arrow.fx.IO
import arrow.fx.extensions.io.async.async
import arrow.fx.reactor.MonoK
import arrow.fx.reactor.extensions.monok.applicativeError.attempt
import arrow.fx.reactor.extensions.monok.async.async
import arrow.fx.reactor.unsafeRunSync
import arrow.fx.rx2.ObservableK
import arrow.fx.rx2.extensions.observablek.async.async
import arrow.integrations.retrofit.adapter.io.TaglessAdapterFactory
import arrow.integrations.retrofit.adapter.retrofit.IOApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.MonoKApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.ObservableKApiClientTest
import arrow.integrations.retrofit.adapter.retrofit.retrofit
import io.kotlintest.fail
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals

class TaglessFailedCallTest : UnitSpec() {

  private fun server(): MockWebServer = MockWebServer().apply {
    enqueue(MockResponse().setBody("{\"response\":  \"hello, world!\"}").setResponseCode(500))
    start()
  }

  private fun baseUrl(): HttpUrl = server().url("/")

  init {
    "should be able to parse answer with IO" {
      val result = createIOApiClientTest(baseUrl())
        .testIO()
        .attempt()
        .unsafeRunSyncEither()

      assertIsException(result)
    }

    "should be able to parse answer with ObservableK" {
      createObservableKApiClientTest(baseUrl())
        .testObservableK()
        .observable
        .test()
        .await()
        .assertError { it.message == "HTTP 500 Server Error" }
    }

    "should be able to parse answer with MonoK" {
      createMonoKApiClientTest(baseUrl())
        .testMonoK()
        .attempt()
        .unsafeRunSync()!!
        .fold({ throwable ->
          assertEquals("HTTP 500 Server Error", throwable.message)
        }, { _ ->
          fail("The requested ended with an success, but should be failed")
        })
    }

    "should be able to run IO POST with UNIT as response" {
      val result = createIOApiClientTest(baseUrl())
        .testUnitResponsePost()
        .attempt()
        .unsafeRunSyncEither()

      assertIsException(result)
    }

    "should be able to run ObservableK POST with UNIT as response" {
      createObservableKApiClientTest(baseUrl())
        .testUnitResponsePost()
        .observable
        .test()
        .await()
        .assertError { it.message == "HTTP 500 Server Error" }
    }

    "should be able to run MonoK POST with UNIT as response" {
      val result = createMonoKApiClientTest(baseUrl())
        .testUnitResponsePost()
        .attempt()
        .unsafeRunSync()

      result!!.fold({ throwable ->
        assertEquals("HTTP 500 Server Error", throwable.message)
      }, {
        fail("The request terminated as success, but should be exception")
      })
    }
  }
}

private fun <R> assertIsException(result: Either<*, Either<Throwable, R>>) {
  result.fold({
    fail("The request terminated with an error (IO left value), but should be exception")
  }, {
    it.fold({ throwable ->
      assertEquals("HTTP 500 Server Error", throwable.message)
    }, { response ->
      fail("The request terminated as success, but should be exception. Response: $response")
    })
  })
}

private fun createIOApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.create(IO.async<Throwable>())).create(IOApiClientTest::class.java)

private fun createObservableKApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.create(ObservableK.async())).create(ObservableKApiClientTest::class.java)

private fun createMonoKApiClientTest(baseUrl: HttpUrl) =
  retrofit(baseUrl, TaglessAdapterFactory.create(MonoK.async())).create(MonoKApiClientTest::class.java)
