package com.lifepad.app.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val securityManager: SecurityManager
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        // App went to background — record the time
        securityManager.setLastBackgroundTime(System.currentTimeMillis())
    }
}
