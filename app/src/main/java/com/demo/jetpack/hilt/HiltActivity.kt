package com.demo.jetpack.hilt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.core.logD
import com.demo.jetpack.databinding.ActivityHiltBinding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject

@AndroidEntryPoint
class HiltActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityHiltBinding

    @Inject
    lateinit var mTruck: Truck

    @Inject
    lateinit var mOkHttpClient: OkHttpClient

    @Inject
    lateinit var mRetrofit: Retrofit

    @Inject
    lateinit var mViewModel: HiltViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHiltBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mTruck.deliver()
        logD { "net work: $mOkHttpClient, $mRetrofit" }
    }

}