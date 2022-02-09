# Arrow Integrations

[![Maven Central](https://img.shields.io/maven-central/v/io.arrow-kt/arrow-integrations-jackson-module?color=4caf50&label=latest%20release)](https://maven-badges.herokuapp.com/maven-central/io.arrow-kt/arrow-integrations-jackson-module)
[![Latest snapshot](https://img.shields.io/maven-metadata/v?label=latest%20snapshot&metadataUrl=https%3A%2F%2Foss.sonatype.org%2Fservice%2Flocal%2Frepositories%2Fsnapshots%2Fcontent%2Fio%2Farrow-kt%2Farrow-integrations-jackson-module%2Fmaven-metadata.xml)](https://oss.sonatype.org/service/local/repositories/snapshots/content/io/arrow-kt/)

[![Arrow Core logo](https://raw.githubusercontent.com/arrow-kt/arrow-site/master/docs/img/core/arrow-core-brand-sidebar.svg?sanitize=true)](https://arrow-kt.io)

Λrrow Integrations is part of [**Λrrow**](https://arrow-kt.io).

Global properties come from [**arrow**](https://github.com/arrow-kt/arrow) repository.

## Jackson Module

To register support for arrow datatypes, simply call `.registerArrowModule()` on the object mapper as follows:

```kotlin:ank
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

```kotlin:ank
import arrow.core.Either
import arrow.core.Ior
import arrow.core.Nel
import arrow.core.Option
import arrow.core.Validated
import arrow.core.bothIor
import arrow.core.nel
import arrow.core.none
import arrow.core.right
import arrow.core.some
import arrow.core.valid
import arrow.integrations.jackson.module.registerArrowModule
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val mapper = ObjectMapper()
    .registerKotlinModule()
    .registerArrowModule()
    .setSerializationInclusion(JsonInclude.Include.NON_ABSENT) // when enabled Option.none() will not be serialized as null

data class Foo(val value: Option<String>)
data class Bar(val value: Nel<String>)
data class Baz(val value: Either<Int, String>)
data class Validation(val value: Validated<Int, String>)
data class IorValue(val value: Ior<Int, String>)

// serialization

mapper.writeValueAsString(Foo(none())) 
// {}

mapper.writeValueAsString(Foo("foo".some())) 
// {"value":"foo"}

mapper.writeValueAsString(Bar("bar".nel())) 
// {"value":["bar"]}

mapper.writeValueAsString(Baz("hello".right()))
// {"value":{"right":"hello"}}

mapper.writeValueAsString(Validation("hello".valid()))
// {"value":{"valid":"hello"}}

mapper.writeValueAsString(IorValue((1 to "hello").bothIor()))
// {"value":{"left":1,"right":"hello"}}

// deserialization

mapper.readValue("{}", Foo::class.java) 
// Foo(value=Option.None)

mapper.readValue("""{"value":"foo"}""", Foo::class.java) 
// Foo(value=Option.Some(foo))

mapper.readValue("""{"value":["bar"]}""", Bar::class.java) 
// Bar(value=NonEmptyList([bar]))

mapper.readValue("""{"value":{"left":5}}""", Baz::class.java)
// Baz(value=Either.Left(5))

mapper.readValue("""{"value":{"invalid":5}}""", Validation::class.java)
// Baz(value=Validated.Invalid(5))

mapper.readValue("""{"value":{"left":5}}""", IorValue::class.java)
// IorValue(value=Ior.Left(5))
```


## Retrofit Adapter

// TODO
