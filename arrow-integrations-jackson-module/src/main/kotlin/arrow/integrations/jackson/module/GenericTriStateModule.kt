package arrow.integrations.jackson.module

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
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
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer
import com.fasterxml.jackson.databind.type.ReferenceType
import com.fasterxml.jackson.databind.type.TypeBindings
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.type.TypeModifier
import com.fasterxml.jackson.databind.util.AccessPattern
import com.fasterxml.jackson.databind.util.NameTransformer
import java.lang.reflect.Type

public class GenericTriStateModule<T>(
  clazz: Class<T>,
  serializationConfig: GenericTriStateSerializationConfig<T>,
  deserializationConfig: GenericTriStateDeserializationConfig<T>,
) :
  SimpleModule(
    "${GenericTriStateModule::class.java.canonicalName}-${clazz.canonicalName}",
    PackageVersion.VERSION
  ) {

  public companion object {
    public inline operator fun <reified T> invoke(
      serializationConfig: GenericTriStateSerializationConfig<T>,
      deserializationConfig: GenericTriStateDeserializationConfig<T>
    ): GenericTriStateModule<T> =
      GenericTriStateModule(T::class.java, serializationConfig, deserializationConfig)
  }

  public val deserializer: GenericTriStateDeserializer<T> =
    GenericTriStateDeserializer(
      clazz,
      deserializationConfig.ifAbsent,
      deserializationConfig.ifNull,
      deserializationConfig.ifDefined
    )

  public val serializerResolver: GenericTriStateSerializerResolver<T> =
    GenericTriStateSerializerResolver(
      clazz,
      serializationConfig.isPresent,
      serializationConfig.serializeValue
    )

  public val typeModifier: GenericTriStateTypeModifier<T> = GenericTriStateTypeModifier(clazz)

  init {
    addDeserializer(clazz, deserializer)
  }

  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addSerializers(serializerResolver)
    context.addTypeModifier(typeModifier)
  }
}

public data class GenericTriStateDeserializationConfig<T>(
  val ifAbsent: () -> T,
  val ifNull: () -> T,
  val ifDefined: (Any) -> T
)

public data class GenericTriStateSerializationConfig<T>(
  val isPresent: (T) -> Boolean,
  val serializeValue: (T) -> SerializedValue
)

public class GenericTriStateSerializerResolver<T>(
  private val clazz: Class<T>,
  private val isPresent: (T) -> Boolean,
  private val serializeValue: (T) -> SerializedValue
) : Serializers.Base() {
  override fun findReferenceSerializer(
    config: SerializationConfig,
    type: ReferenceType,
    beanDesc: BeanDescription?,
    contentTypeSerializer: TypeSerializer?,
    contentValueSerializer: JsonSerializer<Any>?
  ): JsonSerializer<*>? =
    if (clazz.isAssignableFrom(type.rawClass)) {
      val staticTyping =
        (contentTypeSerializer == null && config.isEnabled(MapperFeature.USE_STATIC_TYPING))
      GenericTriStateSerializer(isPresent, serializeValue)
        .createSerializer(type, staticTyping, contentTypeSerializer, contentValueSerializer)
    } else {
      null
    }
}

public class GenericTriStateTypeModifier<T>(private val clazz: Class<T>) : TypeModifier() {
  override fun modifyType(
    type: JavaType,
    jdkType: Type,
    context: TypeBindings?,
    typeFactory: TypeFactory?
  ): JavaType =
    when {
      type.isReferenceType || type.isContainerType -> type
      type.rawClass == clazz -> ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0))
      else -> type
    }
}

public sealed class SerializedValue {
  public object ExplicitNull : SerializedValue()
  public object AbsentOrNull : SerializedValue()
  public data class Value(val value: Any?) : SerializedValue()
}

public class GenericTriStateSerializer<T>(
  private val isPresent: (T) -> Boolean,
  private val serializeValue: (T) -> SerializedValue
) {
  public inner class GenericSerializer : ReferenceTypeSerializer<T> {
    public constructor(
      fullType: ReferenceType,
      staticTyping: Boolean,
      typeSerializer: TypeSerializer?,
      jsonSerializer: JsonSerializer<Any>?
    ) : super(fullType, staticTyping, typeSerializer, jsonSerializer)

    public constructor(
      base: GenericSerializer,
      property: BeanProperty?,
      typeSerializer: TypeSerializer?,
      valueSer: JsonSerializer<*>?,
      unwrapper: NameTransformer?,
      suppressableValue: Any?,
      suppressNulls: Boolean
    ) : super(base, property, typeSerializer, valueSer, unwrapper, suppressableValue, suppressNulls)

    override fun withContentInclusion(
      suppressableValue: Any?,
      suppressNulls: Boolean
    ): ReferenceTypeSerializer<T> =
      GenericSerializer(
        this,
        _property,
        _valueTypeSerializer,
        _valueSerializer,
        _unwrapper,
        suppressableValue,
        suppressNulls
      )

    override fun _isValuePresent(value: T): Boolean = isPresent(value)

    override fun _getReferenced(value: T): Any? =
      when (val serialized = serializeValue(value)) {
        SerializedValue.AbsentOrNull -> null
        SerializedValue.ExplicitNull -> NullNode.getInstance()
        is SerializedValue.Value -> serialized.value
      }

    override fun _getReferencedIfPresent(value: T): Any? = _getReferenced(value)

    override fun withResolved(
      prop: BeanProperty?,
      vts: TypeSerializer?,
      valueSer: JsonSerializer<*>?,
      unwrapper: NameTransformer?
    ): ReferenceTypeSerializer<T> =
      GenericSerializer(this, prop, vts, valueSer, unwrapper, _suppressableValue, _suppressNulls)
  }

  public fun createSerializer(
    fullType: ReferenceType,
    staticTyping: Boolean,
    typeSerializer: TypeSerializer?,
    jsonSerializer: JsonSerializer<Any>?
  ): GenericSerializer = GenericSerializer(fullType, staticTyping, typeSerializer, jsonSerializer)
}

public class GenericTriStateDeserializer<T>(
  private val clazz: Class<T>,
  private val ifAbsent: () -> T,
  private val ifNull: () -> T,
  private val ifDefined: (Any) -> T
) : JsonDeserializer<T>(), ContextualDeserializer {
  private lateinit var valueType: JavaType
  override fun deserialize(p: JsonParser?, ctxt: DeserializationContext): T =
    p.toOption()
      .fold(
        { ifNull() },
        {
          val value = ctxt.readValue<Any>(it, valueType)
          ifDefined(value)
        }
      )

  override fun createContextual(
    ctxt: DeserializationContext,
    property: BeanProperty?
  ): JsonDeserializer<*> {
    val valueType =
      Option.fromNullable(property)
        .map { it.type.containedTypeOrUnknown(0) }
        .orElse { Option.fromNullable(ctxt.contextualType?.containedTypeOrUnknown(0)) }
        .getOrElse { ctxt.constructType(Any::class.java) }

    val deserializer = GenericTriStateDeserializer(clazz, ifAbsent, ifNull, ifDefined)
    deserializer.valueType = valueType
    return deserializer
  }

  override fun getAbsentValue(ctxt: DeserializationContext?): T = ifAbsent()
  override fun getNullValue(ctxt: DeserializationContext): T = ifNull()
  override fun getEmptyValue(ctxt: DeserializationContext?): T = ifAbsent()
  override fun getNullAccessPattern(): AccessPattern = AccessPattern.CONSTANT
  override fun getEmptyAccessPattern(): AccessPattern = AccessPattern.CONSTANT
}
