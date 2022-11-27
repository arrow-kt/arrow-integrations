package arrow.integrations.jackson.module

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import com.fasterxml.jackson.core.json.PackageVersion
import com.fasterxml.jackson.databind.module.SimpleModule

public object OptionModule :
  SimpleModule(OptionModule::class.java.canonicalName, PackageVersion.VERSION) {
  private val module: GenericTriStateModule<Option<*>> =
    GenericTriStateModule(
      serializationConfig =
        GenericTriStateSerializationConfig(
          isPresent = { it.isDefined() },
          serializeValue = {
            it.fold({ SerializedValue.AbsentOrNull }, { value -> SerializedValue.Value(value) })
          }
        ),
      deserializationConfig =
        GenericTriStateDeserializationConfig(
          ifAbsent = { None },
          ifNull = { None },
          ifDefined = { it.some() }
        )
    )

  init {
    addDeserializer(Option::class.java, module.deserializer)
  }

  override fun setupModule(context: SetupContext) {
    super.setupModule(context)
    context.addSerializers(module.serializerResolver)
    context.addTypeModifier(module.typeModifier)
  }
}
