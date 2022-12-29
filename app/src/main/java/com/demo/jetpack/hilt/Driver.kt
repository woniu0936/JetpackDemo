package com.demo.jetpack.hilt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Driver @Inject constructor(@ApplicationContext val context: Context) {
}