package arrow.integrations.jackson.module.common

import arrow.core.Option
import arrow.core.firstOrNone
import arrow.core.toOption
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class UnionTypeDeserializer<T>(
  private val clazz: Class<T>,
  private val fields: List<InjectField<T>>,
) : StdDeserializer<T>(clazz), ContextualDeserializer {
  private data class ElementDeserializer(
    val deserializer: Option<JsonDeserializer<*>>,
    val typeDeserializer: Option<TypeDeserializer>
  )

  private val deserializers: MutableMap<String, ElementDeserializer> = mutableMapOf()

  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): T {
    val javaType = ctxt.contextualType
    fun deserializeValue(token: JsonToken, elementDeserializer: ElementDeserializer): Any? = when (token) {
      JsonToken.VALUE_NULL -> null
      else -> elementDeserializer.deserializer.fold(
        { ctxt.handleUnexpectedToken(javaType.rawClass, token, parser, "no deserializer was found for given type") },
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

    return fields.firstOrNone { parser.currentName == it.fieldName }.fold(
      {
        val validFields = fields.map { it.fieldName }
        val message = "Cannot deserialize $javaType. Make sure the payload contains valid field: $validFields."
        @Suppress("UNCHECKED_CAST")
        ctxt.handleUnexpectedToken(clazz, parser.currentToken, parser, message) as T
      },
      { injectField ->
        deserializeValue(parser.nextToken(), injectField.elementDeserializer()).let { injectField.point(it) }
      }
    )
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

    return UnionTypeDeserializer(clazz, fields).also { deserializer ->
      fields.forEachIndexed { index, field ->
        deserializer.deserializers[field.fieldName] = resolveDeserializer(javaType.containedTypeOrUnknown(index))
      }
    }
  }

  private fun InjectField<*>.elementDeserializer(): ElementDeserializer =
    requireNotNull(deserializers[this.fieldName]) {
      "unexpected deserializer not found"
    }
}

class UnionTypeSerializer<T>(clazz: Class<T>, private val fields: List<ProjectField<T>>) : StdSerializer<T>(clazz) {
  override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider) {
    val project = requireNotNull(fields.firstOrNull { it.getOption(value).isDefined() }) {
      "unexpected failure when attempting projection during serialization"
    }

    gen.writeStartObject()
    project.getOption(value).map {
      provider.defaultSerializeField(project.fieldName, it, gen)
    }
    gen.writeEndObject()
  }
}

class ProjectField<T>(val fieldName: String, val getOption: (T) -> Option<*>)
class InjectField<T>(val fieldName: String, val point: (Any?) -> T)
