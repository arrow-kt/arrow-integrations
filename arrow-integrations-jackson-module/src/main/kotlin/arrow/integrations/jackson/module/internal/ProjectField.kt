package arrow.integrations.jackson.module.internal

import arrow.core.Option

class ProjectField<T>(val fieldName: String, val getOption: (T) -> Option<*>)
