package arrow.integrations.jackson.module.internal

import arrow.core.Option
import arrow.core.toOption
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer

public data class ElementDeserializer(
  val deserializer: Option<JsonDeserializer<*>>,
  val typeDeserializer: Option<TypeDeserializer>
) {
  public companion object {
    public fun resolve(containedType: JavaType, context: DeserializationContext, property: BeanProperty?): ElementDeserializer =
      ElementDeserializer(
        deserializer = context.findContextualValueDeserializer(containedType, property).toOption(),
        typeDeserializer =
          property.toOption().flatMap { prop ->
            BeanDeserializerFactory.instance
              .findPropertyTypeDeserializer(context.config, containedType, prop.member)
              .toOption()
          }
      )
  }

  public fun deserialize(
    javaType: JavaType,
    token: JsonToken,
    parser: JsonParser,
    context: DeserializationContext
  ): Any? =
    when (token) {
      JsonToken.VALUE_NULL -> null
      else ->
        deserializer.fold(
          {
            context.handleUnexpectedToken(
              javaType.rawClass,
              token,
              parser,
              "no deserializer was found for given type"
            )
          },
          { deserializer ->
            typeDeserializer.fold(
              { deserializer.deserialize(parser, context) }, // only deserializer found
              { typeDeserializer ->
                // both deserializer and type deserializer found
                deserializer.deserializeWithType(parser, context, typeDeserializer)
              }
            )
          }
        )
    }
}
