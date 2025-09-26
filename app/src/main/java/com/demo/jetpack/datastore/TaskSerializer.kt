package com.demo.jetpack.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import android.util.Log

object TaskSerializer : Serializer<Task> {
    override val defaultValue: Task = Task.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Task {
        Log.d("TaskSerializer", "Reading from input stream...")
        if (input.available() == 0) {
            Log.d("TaskSerializer", "Input stream is empty, returning default instance.")
            return defaultValue
        }
        try {
            val task = Task.parseFrom(input)
            Log.d("TaskSerializer", "Read Task: ${task.toFormattedString()}")
            return task
        } catch (exception: InvalidProtocolBufferException) {
            Log.e("TaskSerializer", "Error reading Task: ", exception)
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Task, output: OutputStream) {
        Log.d("TaskSerializer", "Writing Task: ${t.toFormattedString()}")
        val codedOutput = CodedOutputStream.newInstance(output)
        t.writeTo(codedOutput)
        codedOutput.flush()
    }
}