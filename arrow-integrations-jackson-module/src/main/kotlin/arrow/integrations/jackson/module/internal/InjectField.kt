package arrow.integrations.jackson.module.internal

class InjectField<T>(val fieldName: String, val point: (Any?) -> T)
