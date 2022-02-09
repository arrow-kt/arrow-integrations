package arrow.integrations.jackson.module.internal

import arrow.core.firstOrNone
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class ProductTypeDeserializer<T>(
  private val clazz: Class<T>,
  private val javaType: JavaType,
  private val fields: List<InjectField<T>>,
  private val fold: (List<T>) -> T
) : StdDeserializer<T>(clazz), ContextualDeserializer {
  private val deserializers: MutableMap<String, ElementDeserializer> = mutableMapOf()

  override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): T {
    val params: MutableList<T> = mutableListOf()
    val introspectedFields: MutableSet<String> = mutableSetOf()

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      fields.firstOrNone { parser.currentName == it.fieldName }.fold(
        {
          val validFields = fields.map { it.fieldName }
          val message = "Cannot deserialize $javaType. Make sure json fields are valid: $validFields."
          @Suppress("UNCHECKED_CAST")
          ctxt.handleUnexpectedToken(clazz, parser.currentToken, parser, message) as T
        },
        { injectField ->
          if (introspectedFields.add(injectField.fieldName)) {
            val elementDeserializer = requireNotNull(deserializers[injectField.fieldName]) {
              "unexpected deserializer not found"
            }
            val value = elementDeserializer.deserialize(javaType, parser.nextToken(), parser, ctxt)
            params.add(injectField.point(value))
          } else {
            val message = "Malformed Json: Field collision were detected for ${parser.currentName}"
            ctxt.handleUnexpectedToken(clazz, parser.currentToken, parser, message)
          }
        }
      )
    }

    return fold(params)
  }

  override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> =
    ProductTypeDeserializer(clazz, javaType, fields, fold).also { deserializer ->
      fields.forEachIndexed { index, field ->
        deserializer.deserializers[field.fieldName] =
          ElementDeserializer.resolve(ctxt.contextualType.containedTypeOrUnknown(index), ctxt, property)
      }
    }
}
