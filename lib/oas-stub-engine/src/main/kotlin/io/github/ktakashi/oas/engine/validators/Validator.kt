package io.github.ktakashi.oas.engine.validators

interface ValidationContext<out T> {
    val target: T
}

interface Validator<T> {
    fun validate(context: ValidationContext<T>): Boolean
    fun shouldValidate(context: ValidationContext<*>): Boolean

    fun tryValidate(context: ValidationContext<T>): Boolean {
        if (shouldValidate(context)) {
            return validate(context)
        }
        return true
    }
}
