package com.budgettracker.ui.viewmodel

class HomeOpenSyncGate {
    private var hasSyncedForCurrentOpen = false

    fun shouldSyncOnHomeOpened(isHomeScreenVisible: Boolean): Boolean {
        if (!isHomeScreenVisible) return false
        if (hasSyncedForCurrentOpen) return false

        hasSyncedForCurrentOpen = true
        return true
    }
}
