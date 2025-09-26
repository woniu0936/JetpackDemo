package com.demo.jetpack.datastore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.demo.jetpack.databinding.ActivityDatastoreBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DataStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDatastoreBinding
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDatastoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                dataStoreManager.saveData(binding.etString.text.toString())
                dataStoreManager.saveData(binding.etInt.text.toString().toIntOrNull() ?: 0)
                dataStoreManager.saveData(binding.etLong.text.toString().toLongOrNull() ?: 0L)
                dataStoreManager.saveData(binding.etFloat.text.toString().toFloatOrNull() ?: 0f)
                dataStoreManager.saveData(binding.etDouble.text.toString().toDoubleOrNull() ?: 0.0)
                dataStoreManager.saveData(binding.cbBoolean.isChecked)
                dataStoreManager.saveData(binding.etStringSet.text.toString().split(",").toSet())
            }
        }

        binding.btnSaveUser.setOnClickListener {
            lifecycleScope.launch {
                val user = User(
                    id = binding.etUserId.text.toString().toIntOrNull() ?: 0,
                    name = binding.etUserName.text.toString(),
                    age = binding.etUserAge.text.toString().toIntOrNull() ?: 0
                )
                dataStoreManager.updateUser(user)
            }
        }

        binding.btnSaveTask.setOnClickListener {
            lifecycleScope.launch {
                val task = Task.newBuilder()
                    .setId(binding.etTaskId.text.toString().toIntOrNull() ?: 0)
                    .setTitle(binding.etTaskTitle.text.toString())
                    .setContent(binding.etTaskContent.text.toString())
                    .build()
                dataStoreManager.updateTask(task)
            }
        }

        lifecycleScope.launch {
            combine(
                dataStoreManager.stringFlow,
                dataStoreManager.intFlow,
                dataStoreManager.longFlow,
                dataStoreManager.floatFlow,
                dataStoreManager.doubleFlow,
                dataStoreManager.booleanFlow,
                dataStoreManager.stringSetFlow,
                dataStoreManager.readUser(),
                dataStoreManager.readTask()
            ) { values ->
                val string = values[0]
                val int = values[1]
                val long = values[2]
                val float = values[3]
                val double = values[4]
                val boolean = values[5]
                val stringSet = values[6]
                val user = values[7] as User
                val task = values[8] as Task

                """
                String: $string
                Int: $int
                Long: $long
                Float: $float
                Double: $double
                Boolean: $boolean
                Set: $stringSet
                User ID: ${user.id}
                User Name: ${user.name}
                User Age: ${user.age}
                User Create Time: ${user.createTime}
                User Modify Time: ${user.modifyTime}
                Task: ${task.toFormattedString()}
                """
            }.collect { allValues ->
                binding.tvSavedValues.text = allValues
            }
        }
    }
}