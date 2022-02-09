package arrow.integrations.jackson.module.internal

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

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
