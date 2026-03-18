package com.lifepad.app.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val securityManager: SecurityManager
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        // Record elapsed-realtime (manipulation-resistant — unaffected by wall-clock changes)
        securityManager.setLastBackgroundTime(SystemClock.elapsedRealtime())
    }
}
