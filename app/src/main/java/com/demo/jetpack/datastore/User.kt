package com.demo.jetpack.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class User(val id: Int, val name: String, val age: Int)

object UserSerializer : Serializer<User> {
    override val defaultValue: User = User(0, "", 0)

    override suspend fun readFrom(input: InputStream): User {
        try {
            return Json.decodeFromString(
                User.serializer(), input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read User", serialization)
        }
    }

    override suspend fun writeTo(t: User, output: OutputStream) {
        output.write(
            Json.encodeToString(User.serializer(), t).encodeToByteArray()
        )
    }
}
