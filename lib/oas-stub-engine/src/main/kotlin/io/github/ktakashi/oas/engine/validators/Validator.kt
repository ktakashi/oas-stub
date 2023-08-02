package io.github.ktakashi.oas.engine.validators

interface Validator<T> {
    fun validate(context: ValidationContext<T>): Boolean
    fun shouldValidate(context: ValidationContext<*>): Boolean

    fun tryValidate(context: ValidationContext<T>, onFailure: (obj: T) -> Unit) {
        if (shouldValidate(context) && validate(context)) {
            onFailure(context.target)
        }
    }
}

interface ValidationContext<T> {
    val target: T
}
