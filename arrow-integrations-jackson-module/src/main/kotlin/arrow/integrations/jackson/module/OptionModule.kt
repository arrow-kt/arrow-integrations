package arrow.integrations.jackson.module

import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.or
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer
import com.fasterxml.jackson.databind.type.ReferenceType
import com.fasterxml.jackson.databind.type.TypeBindings
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.type.TypeModifier
import com.fasterxml.jackson.databind.util.AccessPattern
import com.fasterxml.jackson.databind.util.NameTransformer
import java.lang.reflect.Type

object OptionModule : SimpleModule(PackageVersion.VERSION) {

  init {
    addDeserializer(Option::class.java, OptionDeserializer())
  }

  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addSerializers(OptionSerializerResolver)
    context.addTypeModifier(OptionTypeModifier)
  }
}

object OptionSerializerResolver : Serializers.Base() {
  override fun findReferenceSerializer(
    config: SerializationConfig,
    type: ReferenceType,
    beanDesc: BeanDescription?,
    contentTypeSerializer: TypeSerializer?,
    contentValueSerializer: JsonSerializer<Any>?
  ): JsonSerializer<*>? =
    if (Option::class.java.isAssignableFrom(type.rawClass)) {
      val staticTyping = (contentTypeSerializer == null && config.isEnabled(MapperFeature.USE_STATIC_TYPING))
      OptionSerializer(type, staticTyping, contentTypeSerializer, contentValueSerializer)
    } else {
      null
    }
}

object OptionTypeModifier : TypeModifier() {
  override fun modifyType(type: JavaType, jdkType: Type, context: TypeBindings?, typeFactory: TypeFactory?): JavaType = when {
    type.isReferenceType || type.isContainerType -> type
    type.rawClass == Option::class.java -> ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0))
    else -> type
  }
}

class OptionSerializer : ReferenceTypeSerializer<Option<*>> {
  constructor(fullType: ReferenceType, staticTyping: Boolean, typeSerializer: TypeSerializer?, jsonSerializer: JsonSerializer<Any>?) :
    super(fullType, staticTyping, typeSerializer, jsonSerializer)

  constructor(
    base: OptionSerializer,
    property: BeanProperty?,
    typeSerializer: TypeSerializer?,
    valueSer: JsonSerializer<*>?,
    unwrapper: NameTransformer?,
    suppressableValue: Any?,
    suppressNulls: Boolean
  ) : super(base, property, typeSerializer, valueSer, unwrapper, suppressableValue, suppressNulls)

  override fun withContentInclusion(suppressableValue: Any?, suppressNulls: Boolean): ReferenceTypeSerializer<Option<*>> =
    OptionSerializer(this, _property, _valueTypeSerializer, _valueSerializer, _unwrapper, suppressableValue, suppressNulls)

  override fun _isValuePresent(value: Option<*>): Boolean = value.isDefined()
  override fun _getReferenced(value: Option<*>): Any? = value.orNull()
  override fun _getReferencedIfPresent(value: Option<*>): Any? = value.orNull()
  override fun withResolved(prop: BeanProperty?, vts: TypeSerializer?, valueSer: JsonSerializer<*>?, unwrapper: NameTransformer?):
    ReferenceTypeSerializer<Option<*>> = OptionSerializer(this, prop, vts, valueSer, unwrapper, _suppressableValue, _suppressNulls)
}

class OptionDeserializer : JsonDeserializer<Option<*>>(), ContextualDeserializer {
  private lateinit var valueType: JavaType
  override fun deserialize(p: JsonParser?, ctxt: DeserializationContext): Option<*> =
    Option.fromNullable(p).map {
      ctxt.readValue<Any>(it, valueType)
    }

  override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
    val valueType = Option
      .fromNullable(property)
      .map { it.type.containedTypeOrUnknown(0) }
      .or(Option.fromNullable(ctxt.contextualType?.containedTypeOrUnknown(0)))
      .getOrElse { ctxt.constructType(Any::class.java) }

    val deserializer = OptionDeserializer()
    deserializer.valueType = valueType
    return deserializer
  }

  override fun getNullValue(ctxt: DeserializationContext): Option<*> = None
  override fun getEmptyValue(ctxt: DeserializationContext?): Option<*> = None
  override fun getNullAccessPattern(): AccessPattern = AccessPattern.CONSTANT
  override fun getEmptyAccessPattern(): AccessPattern = AccessPattern.CONSTANT
}
