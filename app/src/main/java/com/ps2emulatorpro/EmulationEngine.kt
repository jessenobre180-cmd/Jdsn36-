package com.ps2emulatorpro

interface Ps2EmulationCore {
    fun initialize(biosUri: String): CoreResult
    fun loadGame(isoUri: String): CoreResult
    fun start(): CoreResult
    fun stop()
}

data class CoreResult(val success: Boolean, val message: String)

class NativeBridge {
    external fun coreStatus(): String
}
