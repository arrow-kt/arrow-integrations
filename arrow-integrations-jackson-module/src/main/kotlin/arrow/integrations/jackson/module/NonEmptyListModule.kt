package arrow.integrations.jackson.module

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.util.StdConverter

object NonEmptyListModule : SimpleModule(PackageVersion.VERSION) {
  init {
    addSerializer(
      NonEmptyList::class.java,
      StdDelegatingSerializer(NonEmptyListSerializationConverter)
    )
  }

  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addDeserializers(NonEmptyListDeserializerResolver)
  }
}

object NonEmptyListSerializationConverter : StdConverter<NonEmptyList<*>, List<*>>() {
  override fun convert(value: NonEmptyList<*>?): List<*>? = value?.all.orEmpty()
}

private class NonEmptyListDeserializationConverter(private val elementType: JavaType) :
  StdConverter<List<Any?>, NonEmptyList<Any?>?>() {

  override fun convert(value: List<*>?): NonEmptyList<*>? =
    value?.let { NonEmptyList.fromList(it).getOrElse { throw IllegalArgumentException("NonEmptyList cannot be empty") } }

  override fun getInputType(typeFactory: TypeFactory): JavaType =
    typeFactory.constructCollectionType(List::class.java, elementType)

  override fun getOutputType(typeFactory: TypeFactory): JavaType =
    typeFactory.constructCollectionLikeType(NonEmptyList::class.java, elementType)
}

object NonEmptyListDeserializerResolver : Deserializers.Base() {

  override fun findCollectionDeserializer(
    type: CollectionType,
    config: DeserializationConfig,
    beanDesc: BeanDescription,
    elementTypeDeserializer: TypeDeserializer?,
    elementDeserializer: JsonDeserializer<*>?
  ): JsonDeserializer<NonEmptyList<*>>? =
    if (NonEmptyList::class.java.isAssignableFrom(type.rawClass)) {
      StdDelegatingDeserializer<NonEmptyList<*>>(NonEmptyListDeserializationConverter(type.bindings.getBoundType(0)))
    } else {
      null
    }
}
