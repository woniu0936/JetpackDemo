package com.demo.jetpack.core.extension

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified VB : ViewBinding> AppCompatActivity.viewBindings(crossinline factory: (LayoutInflater) -> VB) = lazy { factory(layoutInflater) }

//inline fun <reified VB : ViewBinding> Fragment.viewBindings(noinline factory: (View) -> VB) = lazy {
//    FragmentViewBinding(this, factory)
//}
//
//class FragmentViewBinding<VB>(
//    private val owner: LifecycleOwner,
//    val factory: (View) -> VB
//) : ReadOnlyProperty<Fragment, VB>, DefaultLifecycleObserver {
//
//    private var _vb: VB? = null
//
//    private val vb = _vb!!
//
//    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
//        owner.lifecycle.addObserver(this)
//        _vb = thisRef.view?.let {
//            factory(it)
//        }
//        return vb
//    }
//
//
//    override fun onDestroy(owner1: LifecycleOwner) {
//        _vb = null
//        owner.lifecycle.removeObserver(this)
//        super.onDestroy(owner1)
//    }
//
//}