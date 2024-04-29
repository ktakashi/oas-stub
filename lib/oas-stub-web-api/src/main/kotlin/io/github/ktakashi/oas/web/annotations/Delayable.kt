package io.github.ktakashi.oas.web.annotations

/**
 * Delayable annotation.
 *
 * This annotation can be used to delay custom (Rest)Controller.
 *
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
@MustBeDocumented
annotation class Delayable(val context: String, val path: String)
