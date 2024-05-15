package io.github.ktakashi.oas.storages.apis.conditions

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

@Conditional(ConditionalOnPropertyIsEmpty.OnPropertyIsEmptyCondition::class)
annotation class ConditionalOnPropertyIsEmpty(val value: String) {
    class OnPropertyIsEmptyCondition: Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
            val attrs = metadata.getAnnotationAttributes(ConditionalOnPropertyIsEmpty::class.java.name)
                    ?: return true
            val propertyName = attrs["value"] as String?
            val value = propertyName?.let { v -> context.environment.getProperty(v) } ?: return true
            return value.isBlank()
        }
    }
}