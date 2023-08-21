package io.github.ktakashi.oas.storages.mongodb

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import java.time.Duration
import java.util.Optional
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonProperty

abstract class MongodbStorage<T>(mongoClient: MongoClient,
                                 database: String,
                                 collection: String,
                                 clazz: Class<T>) {
    private val pojoCodevProvider = PojoCodecProvider.builder().automatic(true).build()
    private val pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodevProvider))
    private val mongoDatabase = mongoClient.getDatabase(database).withCodecRegistry(pojoCodecRegistry)
    protected val mongoCollection: MongoCollection<T> = mongoDatabase.getCollection(collection, clazz)
}

class MongodbSessionStorage(private val objectMapper: ObjectMapper, mongoClient: MongoClient, database: String, collection: String)
    : MongodbStorage<SessionEntry>(mongoClient, database, collection, SessionEntry::class.java), SessionStorage {
    override fun <T> put(key: String, value: T, ttl: Duration): Boolean {
        // TODO ttl
        val jsonValue = objectMapper.writeValueAsString(value)
        return mongoCollection.updateOne(eq("name", key),
                Updates.set("value", jsonValue),
                UpdateOptions().upsert(true))
                .wasAcknowledged()
    }

    override fun <T : Any> get(key: String, type: Class<T>): Optional<T> = Optional.ofNullable(mongoCollection.find(eq("name", key)).first())
            .map { e -> e.value }
            .map { v -> objectMapper.readValue(v, type) }

    override fun delete(key: String): Boolean = mongoCollection.deleteOne(eq("name", key)).wasAcknowledged()

}

class MongodbPersistentStorage(mongoClient: MongoClient, database: String, collection: String)
    : MongodbStorage<PersistentEntry>(mongoClient, database, collection, PersistentEntry::class.java), PersistentStorage {
    override fun getApiDefinition(applicationName: String): Optional<ApiDefinitions> =
            Optional.ofNullable(mongoCollection.find(eq("name", applicationName)).first())
                    .map { e -> e.definitions }

    override fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions): Boolean =
        mongoCollection.updateOne(eq("name", applicationName),
                Updates.set("definitions", apiDefinitions),
                UpdateOptions().upsert(true))
                .wasAcknowledged()

    override fun deleteApiDefinition(name: String): Boolean =
            mongoCollection.deleteOne(eq("name", name)).wasAcknowledged()
}

data class SessionEntry
@BsonCreator constructor(@BsonProperty("name") val name: String, @BsonProperty("value") val value: String)
data class PersistentEntry
@BsonCreator constructor(@BsonProperty("name") val name: String, @BsonProperty("definitions") val definitions: ApiDefinitions)
