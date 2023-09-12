package io.github.ktakashi.oas.web.annotations

import kotlin.annotation.AnnotationTarget

@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
annotation class Admin

