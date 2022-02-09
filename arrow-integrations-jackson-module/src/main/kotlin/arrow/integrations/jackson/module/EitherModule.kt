package arrow.integrations.jackson.module

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.integrations.jackson.module.internal.InjectField
import arrow.integrations.jackson.module.internal.ProjectField
import arrow.integrations.jackson.module.internal.UnionTypeSerializer
import arrow.integrations.jackson.module.internal.UnionTypeDeserializer
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

class EitherSerializerResolver(leftFieldName: String, rightFieldName: String) : Serializers.Base() {
  private val serializer = UnionTypeSerializer(
    Either::class.java,
    listOf(
      ProjectField(leftFieldName) { either -> either.swap().orNone() },
      ProjectField(rightFieldName) { either -> either.orNone() }
    ),
  )

  override fun findSerializer(
    config: SerializationConfig,
    javaType: JavaType,
    beanDesc: BeanDescription?
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
    Either::class.java.isAssignableFrom(type.rawClass) -> UnionTypeDeserializer(
      Either::class.java,
      type,
      listOf(
        InjectField(leftFieldName) { leftValue -> leftValue.left() },
        InjectField(rightFieldName) { rightValue -> rightValue.right() }
      )
    )
    else -> null
  }
}
