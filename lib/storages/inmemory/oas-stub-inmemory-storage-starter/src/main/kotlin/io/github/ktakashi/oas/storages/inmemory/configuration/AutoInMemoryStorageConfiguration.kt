package io.github.ktakashi.oas.storages.inmemory.configuration

import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@ConditionalOnProperty(name = [ "oas.storage.persistent" ], havingValue = "in-memory")
class AutoInMemoryPersistentStorageConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun persistentStorage(): PersistentStorage = InMemoryStorage()
}

@AutoConfiguration
@Configuration
@ConditionalOnProperty(name = [ "oas.storage.session" ], havingValue = "in-memory")
class AutoInMemorySessionStorageConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun sessionStorage(): SessionStorage = InMemoryStorage()
}

