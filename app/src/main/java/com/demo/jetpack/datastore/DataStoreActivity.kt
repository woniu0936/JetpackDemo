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

        lifecycleScope.launch {
            combine(
                dataStoreManager.stringFlow,
                dataStoreManager.intFlow,
                dataStoreManager.longFlow,
                dataStoreManager.floatFlow,
                dataStoreManager.doubleFlow,
                dataStoreManager.booleanFlow,
                dataStoreManager.stringSetFlow
            ) { values ->
                val string = values[0]
                val int = values[1]
                val long = values[2]
                val float = values[3]
                val double = values[4]
                val boolean = values[5]
                val stringSet = values[6]

                """
                String: $string
                Int: $int
                Long: $long
                Float: $float
                Double: $double
                Boolean: $boolean
                Set: $stringSet
                """
            }.collect { allValues ->
                binding.tvSavedValues.text = allValues
            }
        }
    }
}