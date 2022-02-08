package arrow.integrations.jackson.module

import com.fasterxml.jackson.databind.ObjectMapper

fun ObjectMapper.registerArrowModule(
  eitherModuleConfig: EitherModuleConfig = EitherModuleConfig.default(),
): ObjectMapper = registerModules(
  NonEmptyListModule,
  OptionModule,
  EitherModule(eitherModuleConfig.leftFieldName, eitherModuleConfig.rightFieldName)
)

data class EitherModuleConfig(val leftFieldName: String, val rightFieldName: String) {
  companion object {
    fun default(): EitherModuleConfig = EitherModuleConfig("left", "right")
  }
}
