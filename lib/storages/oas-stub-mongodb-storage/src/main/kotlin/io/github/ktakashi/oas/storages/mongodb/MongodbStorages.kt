package io.github.ktakashi.oas.storages.mongodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import java.time.Duration
import java.util.Optional
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonProperty

abstract class MongodbStorage(private val objectMapper: ObjectMapper,
                              mongoClient: MongoClient,
                              database: String,
                              collection: String) {
    private val pojoCodevProvider = PojoCodecProvider.builder().automatic(true).build()
    private val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodevProvider))
    private val mongoDatabase = mongoClient.getDatabase(database).withCodecRegistry(pojoCodecRegistry)
    protected val mongoCollection: MongoCollection<MongoEntry> = mongoDatabase.getCollection(collection, MongoEntry::class.java)
    protected val updateOptions = UpdateOptions().upsert(true)

    protected fun <T> upsert(key: String, value: T) = objectMapper.writeValueAsString(value).let {
        mongoCollection.updateOne(eq("name", key), Updates.set("value", it), updateOptions)
    }

    protected fun <T : Any> findOne(key: String, type: Class<T>): Optional<T> = Optional.ofNullable(mongoCollection.find(eq("name", key)).first())
            .map { e -> e.value }
            .map { v -> objectMapper.readValue(v, type)}

    protected fun deleteOne(key: String) = mongoCollection.deleteOne(eq("name", key))
}

class MongodbSessionStorage(objectMapper: ObjectMapper, mongoClient: MongoClient, database: String, collection: String)
    : MongodbStorage(objectMapper, mongoClient, database, collection), SessionStorage {

    // TODO ttl
    override fun <T> put(key: String, value: T, ttl: Duration): Boolean = upsert(key, value).wasAcknowledged()

    override fun <T : Any> get(key: String, type: Class<T>) = findOne(key, type)

    override fun delete(key: String): Boolean = deleteOne(key).wasAcknowledged()

}

class MongodbPersistentStorage(objectMapper: ObjectMapper, mongoClient: MongoClient, database: String, collection: String)
    : MongodbStorage(objectMapper, mongoClient, database, collection), PersistentStorage {
    override fun getApiDefinition(applicationName: String): Optional<ApiDefinitions> = findOne(applicationName, ApiDefinitions::class.java)

    override fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions): Boolean = upsert(applicationName, apiDefinitions).wasAcknowledged()

    override fun deleteApiDefinition(name: String): Boolean = deleteOne(name).wasAcknowledged()
}

data class MongoEntry
@BsonCreator constructor(@BsonProperty("name") val name: String, @BsonProperty("value") val value: String)
