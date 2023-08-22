package io.github.ktakashi.oas.storages.inmemory.configurations

import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@Configuration
@ConditionalOnProperty(name = [ "oas.storage.type.persistent" ], havingValue = "inmemory", matchIfMissing = true)
@ConditionalOnMissingBean(value = [PersistentStorage::class])
class AutoInMemoryPersistentStorageConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun persistentStorage(): PersistentStorage = InMemoryPersistentStorage()
}

@AutoConfiguration
@Configuration
@ConditionalOnProperty(name = [ "oas.storage.type.session" ], havingValue = "inmemory", matchIfMissing = true)
@ConditionalOnMissingBean(value = [SessionStorage::class])
class AutoInMemorySessionStorageConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun sessionStorage(): SessionStorage = InMemorySessionStorage()
}

