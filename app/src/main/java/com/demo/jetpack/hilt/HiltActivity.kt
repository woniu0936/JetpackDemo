package com.demo.jetpack.hilt

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.demo.jetpack.extension.logD
import com.demo.jetpack.databinding.ActivityHiltBinding
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject

//@AndroidEntryPoint注解使Hilt 能将依赖项注入到 Activity 中
//@AndroidEntryPoint 注解可以添加到绝大部分 Android 框架类上，不仅仅是 Activity。它会为被添加注解的类去创建一个依赖项容器的实例，并填充所有添加了 @Inject 注解的变量。
@AndroidEntryPoint
class HiltActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityHiltBinding

    //当 @Inject 注解被添加到类的构造函数上时，它会告诉 Hilt 如何提供该类的实例。当变量被添加 @Inject 注解，并且变量所属的类被添加 @AndroidEntryPoint 注解时，Hilt 会向该类中注入一个相应类型的实例。
    @Inject
    lateinit var mTruck: Truck

    @Inject
    lateinit var mOkHttpClient: OkHttpClient

    @Inject
    lateinit var mRetrofit: Retrofit

    private val mViewModel: HiltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHiltBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mTruck.deliver()
        logD { "inject object: $mOkHttpClient, $mRetrofit, $mViewModel" }
    }

}