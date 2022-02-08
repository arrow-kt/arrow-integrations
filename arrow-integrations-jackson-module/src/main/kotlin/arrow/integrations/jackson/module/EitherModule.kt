package arrow.integrations.jackson.module

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class EitherModule(
  private val leftFieldName: String,
  private val rightFieldName: String
) : SimpleModule(EitherModule::class.java.canonicalName, PackageVersion.VERSION) {
  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addDeserializers(EitherDeserializerResolver(leftFieldName, rightFieldName))
    context.addSerializers(EitherSerializerResolver(leftFieldName, rightFieldName))
  }
}


class EitherSerializerResolver(
  leftFieldName: String,
  rightFieldName: String
) : Serializers.Base() {
  private val serializer = EitherSerializer(leftFieldName, rightFieldName)
  override fun findSerializer(
    config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription?
  ): JsonSerializer<*>? = when {
    Either::class.java.isAssignableFrom(javaType.rawClass) -> serializer
    else -> null
  }
}

class EitherDeserializerResolver(
  private val leftFieldName: String,
  private val rightFieldName: String
) : Deserializers.Base() {
  override fun findBeanDeserializer(
    type: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription?
  ): JsonDeserializer<*>? = when {
    Either::class.java.isAssignableFrom(type.rawClass) -> EitherDeserializer(config, leftFieldName, rightFieldName)
    else -> null
  }
}

class EitherDeserializer(
  private val config: DeserializationConfig,
  private val leftFieldName: String,
  private val rightFieldName: String
) : StdDeserializer<Either<*, *>>(Either::class.java), ContextualDeserializer {
  private data class ElementDeserializer(
    val deserializer: Option<JsonDeserializer<*>>,
    val typeDeserializer: Option<TypeDeserializer>
  )

  private lateinit var leftDeserializer: ElementDeserializer
  private lateinit var rightDeserializer: ElementDeserializer

  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Either<*, *> {
    val javaType = ctxt.contextualType
    fun deserializeValue(token: JsonToken, elementDeserializer: ElementDeserializer): Any? = when (token) {
      JsonToken.VALUE_NULL -> null
      else -> elementDeserializer.deserializer.fold(
        { ctxt.handleUnexpectedToken(javaType.rawClass, parser) }, // no deserializer was found for given type
        { deserializer ->
          elementDeserializer.typeDeserializer.fold(
            { deserializer.deserialize(parser, ctxt) }, // only deserializer found
            { typeDeserializer ->
              // both deserializer and type deserializer found
              deserializer.deserializeWithType(parser, ctxt, typeDeserializer)
            }
          )
        }
      )
    }

    parser.nextToken()

    return when (parser.currentName) {
      leftFieldName -> deserializeValue(parser.nextToken(), leftDeserializer).left()
      rightFieldName -> deserializeValue(parser.nextToken(), rightDeserializer).right()
      else -> ctxt.handleUnexpectedToken(ctxt.contextualType.rawClass, parser) as Either<*, *>
    }
  }

  override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
    val javaType = ctxt.contextualType
    fun resolveDeserializer(containedType: JavaType): ElementDeserializer = ElementDeserializer(
      deserializer = ctxt.findContextualValueDeserializer(containedType, property).toOption(),
      typeDeserializer = property.toOption().flatMap { prop ->
        BeanDeserializerFactory.instance
          .findPropertyTypeDeserializer(ctxt.config, containedType, prop.member)
          .toOption()
      }
    )

    return EitherDeserializer(config, leftFieldName, rightFieldName).also { deserializer ->
      deserializer.leftDeserializer = resolveDeserializer(javaType.containedTypeOrUnknown(0))
      deserializer.rightDeserializer = resolveDeserializer(javaType.containedTypeOrUnknown(1))
    }
  }
}

class EitherSerializer(
  private val leftFieldName: String,
  private val rightFieldName: String
) : StdSerializer<Either<*, *>>(Either::class.java) {
  override fun serialize(value: Either<*, *>, gen: JsonGenerator, provider: SerializerProvider) {
    gen.writeStartObject()
    value.fold(
      { leftValue -> provider.defaultSerializeField(leftFieldName, leftValue, gen) },
      { rightValue -> provider.defaultSerializeField(rightFieldName, rightValue, gen) }
    )
    gen.writeEndObject()
  }
}
