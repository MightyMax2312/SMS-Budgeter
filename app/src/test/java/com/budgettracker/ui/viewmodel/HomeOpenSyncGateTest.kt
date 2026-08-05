package com.budgettracker.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeOpenSyncGateTest {

    @Test
    fun allowsSyncFirstTimeHomeScreenIsOpened() {
        val gate = HomeOpenSyncGate()

        assertTrue(gate.shouldSyncOnHomeOpened(isHomeScreenVisible = true))
    }

    @Test
    fun doesNotSyncBeforeHomeScreenIsVisible() {
        val gate = HomeOpenSyncGate()

        assertFalse(gate.shouldSyncOnHomeOpened(isHomeScreenVisible = false))
    }

    @Test
    fun allowsOnlyOneSyncPerGateInstance() {
        val gate = HomeOpenSyncGate()

        gate.shouldSyncOnHomeOpened(isHomeScreenVisible = true)

        assertFalse(gate.shouldSyncOnHomeOpened(isHomeScreenVisible = true))
    }
}
