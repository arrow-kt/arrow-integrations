package arrow.integrations.jackson.module.internal

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class ProductTypeSerializer<T>(clazz: Class<T>, private val fields: List<ProjectField<T>>) : StdSerializer<T>(clazz) {
  override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider) {
    gen.writeStartObject()
    fields.forEach { projector ->
      projector.getOption(value).map {
        provider.defaultSerializeField(projector.fieldName, it, gen)
      }
    }
    gen.writeEndObject()
  }
}
