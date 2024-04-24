package io.github.ktakashi.oas.guice.services

import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.web.annotations.Delayable
import io.github.ktakashi.oas.web.aspects.DelayableAspect
import io.github.ktakashi.oas.web.services.ExecutorProvider
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Provider
import jakarta.inject.Singleton
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import org.aopalliance.intercept.ConstructorInterceptor
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.glassfish.hk2.api.Filter
import org.glassfish.hk2.api.InterceptionService
import org.glassfish.hk2.utilities.BuilderHelper
import org.glassfish.hk2.utilities.binding.AbstractBinder

class DelayableInterceptorBinder: AbstractBinder() {
    override fun configure() {
        bind(DelayableInterceptionService::class.java).to(InterceptionService::class.java).`in`(Singleton::class.java)
    }

}

@Named @Singleton
class DelayableInterceptionService
@Inject constructor(private val interceptorProvider: Provider<DelayableInterceptor>): InterceptionService {
    override fun getDescriptorFilter(): Filter = BuilderHelper.allFilter()

    override fun getMethodInterceptors(method: Method): List<MethodInterceptor> = if (method.isAnnotationPresent(
            Delayable::class.java)) {
        listOf(interceptorProvider.get())
    } else {
        listOf()
    }

    override fun getConstructorInterceptors(constructor: Constructor<*>): List<ConstructorInterceptor> = listOf()
}

class DelayableInterceptor: MethodInterceptor {
    @Inject
    lateinit var apiDelayService: ApiDelayService
    @Inject
    lateinit var executorProvider: ExecutorProvider
    private val delayAspect: DelayableAspect by lazy {
        DelayableAspect(apiDelayService, executorProvider)
    }
    override fun invoke(invocation: MethodInvocation): Any? {
        val start = System.currentTimeMillis()
        return invocation.method.getAnnotation(Delayable::class.java)?.let { annotation ->
            delayAspect.proceedDelay(invocation.proceed(), annotation, start)
        } ?: invocation.proceed()
    }

}