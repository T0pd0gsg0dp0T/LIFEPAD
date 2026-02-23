package com.lifepad.app

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class LifepadTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        System.setProperty("lifepad.skipPin", "true")
        super.onCreate(arguments)
    }
}
