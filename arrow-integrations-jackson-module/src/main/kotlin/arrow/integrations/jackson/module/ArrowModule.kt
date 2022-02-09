package arrow.integrations.jackson.module

import com.fasterxml.jackson.databind.ObjectMapper

fun ObjectMapper.registerArrowModule(
  eitherModuleConfig: EitherModuleConfig = EitherModuleConfig.default(),
  validatedModuleConfig: ValidatedModuleConfig = ValidatedModuleConfig.default()
): ObjectMapper = registerModules(
  NonEmptyListModule,
  OptionModule,
  EitherModule(eitherModuleConfig.leftFieldName, eitherModuleConfig.rightFieldName),
  ValidatedModule(validatedModuleConfig.invalidFieldName, validatedModuleConfig.validFieldName)
)

data class EitherModuleConfig(val leftFieldName: String, val rightFieldName: String) {
  companion object {
    fun default(): EitherModuleConfig = EitherModuleConfig("left", "right")
  }
}
data class ValidatedModuleConfig(val invalidFieldName: String, val validFieldName: String) {
  companion object {
    fun default(): ValidatedModuleConfig = ValidatedModuleConfig("invalid", "valid")
  }
}
