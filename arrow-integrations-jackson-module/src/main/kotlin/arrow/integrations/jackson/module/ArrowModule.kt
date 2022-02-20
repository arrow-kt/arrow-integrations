package arrow.integrations.jackson.module

import com.fasterxml.jackson.databind.ObjectMapper

fun ObjectMapper.registerArrowModule(
  eitherModuleConfig: EitherModuleConfig = EitherModuleConfig("left", "right"),
  validatedModuleConfig: ValidatedModuleConfig = ValidatedModuleConfig("invalid", "valid"),
  iorModuleConfig: IorModuleConfig = IorModuleConfig("left", "right")
): ObjectMapper = registerModules(
  NonEmptyListModule,
  OptionModule,
  EitherModule(eitherModuleConfig.leftFieldName, eitherModuleConfig.rightFieldName),
  ValidatedModule(validatedModuleConfig.invalidFieldName, validatedModuleConfig.validFieldName),
  IorModule(iorModuleConfig.leftFieldName, iorModuleConfig.rightFieldName)
)

data class EitherModuleConfig(val leftFieldName: String, val rightFieldName: String)
data class ValidatedModuleConfig(val invalidFieldName: String, val validFieldName: String)
data class IorModuleConfig(val leftFieldName: String, val rightFieldName: String)
