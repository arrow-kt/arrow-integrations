package arrow.integrations.jackson.module

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.orNone
import arrow.core.valid
import arrow.integrations.jackson.module.internal.InjectField
import arrow.integrations.jackson.module.internal.ProjectField
import arrow.integrations.jackson.module.internal.UnionTypeDeserializer
import arrow.integrations.jackson.module.internal.UnionTypeSerializer
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers

class ValidatedModule(
  private val invalidFieldName: String,
  private val validFieldName: String
) : SimpleModule(ValidatedModule::class.java.canonicalName, PackageVersion.VERSION) {
  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addDeserializers(ValidatedDeserializerResolver(invalidFieldName, validFieldName))
    context.addSerializers(ValidatedSerializerResolver(invalidFieldName, validFieldName))
  }
}

class ValidatedSerializerResolver(invalidFieldName: String, validFieldName: String) : Serializers.Base() {
  private val serializer = UnionTypeSerializer(
    Validated::class.java,
    listOf(
      ProjectField(invalidFieldName) { validated -> validated.swap().orNone() },
      ProjectField(validFieldName) { validated -> validated.orNone() },
    )
  )

  override fun findSerializer(
    config: SerializationConfig,
    type: JavaType,
    beanDesc: BeanDescription?
  ): JsonSerializer<*>? = when {
    Validated::class.java.isAssignableFrom(type.rawClass) -> serializer
    else -> null
  }
}

class ValidatedDeserializerResolver(
  private val invalidFieldName: String,
  private val validFieldName: String
) : Deserializers.Base() {
  override fun findBeanDeserializer(
    type: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription?
  ): JsonDeserializer<*>? = when {
    Validated::class.java.isAssignableFrom(type.rawClass) -> UnionTypeDeserializer(
      Validated::class.java,
      type,
      listOf(
        InjectField(invalidFieldName) { invalidValue -> invalidValue.invalid() },
        InjectField(validFieldName) { validValue -> validValue.valid() },
      )
    )
    else -> null
  }
}
