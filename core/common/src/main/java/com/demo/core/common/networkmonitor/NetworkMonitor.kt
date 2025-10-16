package com.novel.app.app.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {

    public val isOnline: Flow<Boolean>

    fun ConnectivityManager.isCurrentlyConnected() = activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        ?: false

    fun Context.isCurrentlyConnected() = this.getSystemService<ConnectivityManager>()
        ?.isCurrentlyConnected()
        ?: false

}