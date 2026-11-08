package com.jksalcedo.passvault.core.util

object SessionManager {
    var isUnlocked: Boolean = false

    fun setUnlocked() {
        isUnlocked = true
    }
}
