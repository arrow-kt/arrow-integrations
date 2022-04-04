package arrow.integrations.jackson.module

import com.fasterxml.jackson.databind.ObjectMapper

public fun ObjectMapper.registerArrowModule(
  eitherModuleConfig: EitherModuleConfig = EitherModuleConfig("left", "right"),
  validatedModuleConfig: ValidatedModuleConfig = ValidatedModuleConfig("invalid", "valid"),
  iorModuleConfig: IorModuleConfig = IorModuleConfig("left", "right")
): ObjectMapper =
  registerModules(
    NonEmptyListModule,
    OptionModule,
    EitherModule(eitherModuleConfig.leftFieldName, eitherModuleConfig.rightFieldName),
    ValidatedModule(validatedModuleConfig.invalidFieldName, validatedModuleConfig.validFieldName),
    IorModule(iorModuleConfig.leftFieldName, iorModuleConfig.rightFieldName)
  )

public data class EitherModuleConfig(val leftFieldName: String, val rightFieldName: String)

public data class ValidatedModuleConfig(val invalidFieldName: String, val validFieldName: String)

public data class IorModuleConfig(val leftFieldName: String, val rightFieldName: String)
