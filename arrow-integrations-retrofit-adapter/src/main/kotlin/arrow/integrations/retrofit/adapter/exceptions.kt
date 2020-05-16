package arrow.integrations.retrofit.adapter

class NullBodyException : IllegalStateException("Null body found!")

class FailedToConvertBodyException(override val cause: Throwable? = null) : IllegalStateException("Failed to convert body!")
