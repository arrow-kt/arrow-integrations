# Arrow Integrations

[![Maven Central](https://img.shields.io/maven-central/v/io.arrow-kt/arrow-integrations-jackson-module?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.arrow-kt/arrow-integrations-jackson-module)
[![Latest snapshot](https://img.shields.io/maven-metadata/v?label=latest%20snapshot&metadataUrl=https%3A%2F%2Foss.sonatype.org%2Fservice%2Flocal%2Frepositories%2Fsnapshots%2Fcontent%2Fio%2Farrow-kt%2Farrow-integrations-jackson-module%2Fmaven-metadata.xml)](https://oss.sonatype.org/service/local/repositories/snapshots/content/io/arrow-kt/)

[![Arrow Core logo](https://raw.githubusercontent.com/arrow-kt/arrow-site/master/docs/img/core/arrow-core-brand-sidebar.svg?sanitize=true)](https://arrow-kt.io)

Λrrow Integrations is part of [**Λrrow**](https://arrow-kt.io).

## Jackson Module

Include `arrow-integrations-jackson` in your gradle project:
```groovy
implementation 'io.arrow-kt:arrow-integrations-jackson-module:${version}'
```
or, using gradle kotlin-dsl.
```kotlin
implementation("io.arrow-kt:arrow-integrations-jackson-module:${version}")
```

Include `arrow-integrations-jackson` in your maven project:
```xml
<dependency>
  <groupId>io.arrow-kt</groupId>
  <artifactId>arrow-integrations-jackson-module</artifactId>
  <version>${version}</version>
</dependency>
```

To register support for arrow datatypes, simply call `.registerArrowModule()` on the object mapper as follows:

```kotlin
import arrow.integrations.jackson.module.registerArrowModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val mapper = ObjectMapper()
    .registerKotlinModule()
    .registerArrowModule()
```

currently supported datatypes:
- `Option<T>`
- `NonEmptyList<T>` or `Nel<T>`
- `Either<L, R>`
- `Validated<E, A>`
- `Ior<L, R>`

### Example Usage

Serialization and deserialization of data classes that incorporate arrow data types can be
done as follows. 

```kotlin
val mapper = ObjectMapper()
  .registerKotlinModule()
  .registerArrowModule()
  .setSerializationInclusion(JsonInclude.Include.NON_ABSENT) // will not serialize None as nulls

data class Organization(val name: String, val address: Option<String>, val websiteUrl: Option<URI>)
data class ArrowUser(val name: String, val emails: NonEmptyList<String>, val organization: Option<Organization>)

val arrowUser = ArrowUser(
  "John Doe",
  nonEmptyListOf(
    "john@email.com", 
    "john.doe@email.com.au"
  ), 
  Organization("arrow-kt", none(), URI("https://arrow-kt.io").some()).some()
)

mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user)
```
which serializes as follows.
```json
{
  "name" : "John Doe",
  "emails" : [ "john@email.com", "john.doe@email.com.au" ],
  "organization" : {
    "name" : "arrow-kt",
    "websiteUrl" : "https://arrow-kt.io"
  }
}
```
Notice that the `Option<T>` serializer
is configurable via Jackson's serialization inclusion setting. In this example we have configured the serializer
to not serialize `none()` as null, but instead omit it completely.

Various serializers / deserializers within arrow module are configurable.
For instance the field names used for serializing / deserializing `Either`, `Validated` or `Ior` can
be configured within the registration step:

```kotlin
val mapper: ObjectMapper = ObjectMapper()
  .registerKotlinModule()
  .registerArrowModule(
    eitherModuleConfig = EitherModuleConfig("left", "right"),           // sets the field names for either left / right
    validatedModuleConfig = ValidatedModuleConfig("invalid", "valid"),  // sets the field names for validated invalid / valid
    iorModuleConfig = IorModuleConfig("left", "right")                  // sets the field names for ior left / right
  )
  .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)            // do not serialize None as nulls

```

More example usages can be found in [ExampleTest.kt](arrow-integrations-jackson-module/src/test/kotlin/arrow/integrations/jackson/module/ExampleTest.kt)

### Example Usage for Popular Web Frameworks

In real world scenarios Jackson can be installed as the json serialization/deserialization
engine. These serializations / deserializations are normally done
automatically.

For instance we can customize our mapper with `.registerArrowModule()` as follows.
```kotlin
object JsonMapper { 
  val mapper: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule(singletonSupport = SingletonSupport.CANONICALIZE))
    .registerArrowModule()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION)
    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
}
```
This can then be installed accordingly.

#### Spring Boot

A way to register of arrow data types JSON serialization / deserialization support in spring boot is as follows:

```kotlin
@Configuration
class JacksonConfiguration {

  @Bean
  @Primary
  fun jsonMapper(): ObjectMapper = JsonMapper.mapper
}
```
When this bean is registered, the object mapper will be used to deserialize incoming and outgoing JSON payload.

#### Ktor

Jackson support for arrow data type serialization / deserialization can similarly be registered in Ktor as follows:
```kotlin
install(ContentNegotiation) {
  register(ContentType.Application.Json, JacksonConverter(JsonMapper.mapper))
}
```
