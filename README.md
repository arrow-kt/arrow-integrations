# Arrow Integrations

[![Maven Central](https://img.shields.io/maven-central/v/io.arrow-kt/arrow-integrations-jackson-module?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.arrow-kt/arrow-integrations-jackson-module)
[![Latest snapshot](https://img.shields.io/maven-metadata/v?label=latest%20snapshot&metadataUrl=https%3A%2F%2Foss.sonatype.org%2Fservice%2Flocal%2Frepositories%2Fsnapshots%2Fcontent%2Fio%2Farrow-kt%2Farrow-integrations-jackson-module%2Fmaven-metadata.xml)](https://oss.sonatype.org/service/local/repositories/snapshots/content/io/arrow-kt/)

[![Arrow Core logo](https://raw.githubusercontent.com/arrow-kt/arrow-site/master/docs/img/core/arrow-core-brand-sidebar.svg?sanitize=true)](https://arrow-kt.io)

Λrrow Integrations is part of [**Λrrow**](https://arrow-kt.io).

Global properties come from [**arrow**](https://github.com/arrow-kt/arrow) repository.

## Jackson Module

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

### Example usage

Serialization and deserialization of data classes with arrow data types are supported.

```kotlin
val mapper = ObjectMapper()
  .registerKotlinModule()
  .registerArrowModule()
  .setSerializationInclusion(JsonInclude.Include.NON_ABSENT) // will not serialize None as nulls

data class Organization(val name: String, val address: Option<String>, val websiteUrl: Option<URI>)
data class ArrowUser(val name: String, val emails: NonEmptyList<String>, val organization: Option<Organization>)

val arrowKt = Organization("arrow-kt", none(), URI("https://arrow-kt.io").some())
val arrowUser = ArrowUser(
  "John Doe",
  nonEmptyListOf("john@email.com", "john.doe@email.com.au"),
  arrowKt.some()
)

val prettyPrinter = mapper.writerWithDefaultPrettyPrinter()
prettyPrinter.writeValueAsString(user)
// {
//   "name" : "John Doe",
//   "emails" : [ "john@email.com", "john.doe@email.com.au" ],
//   "organization" : {
//     "name" : "arrow-kt",
//     "websiteUrl" : "https://arrow-kt.io"
//   }
// }

val validOrganization: Validated<Nel<String>, Organization> = arrowKt.valid()
prettyPrinter.writeValueAsString(validOrganization)
// {
//   "valid" : {
//     "name" : "arrow-kt",
//     "websiteUrl" : "https://arrow-kt.io"
//   }
// }
```
