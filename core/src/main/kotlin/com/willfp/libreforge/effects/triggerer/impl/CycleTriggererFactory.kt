package com.willfp.libreforge.effects.triggerer.impl

import com.willfp.libreforge.effects.Chain
import com.willfp.libreforge.effects.triggerer.ChainTriggerer
import com.willfp.libreforge.effects.triggerer.ChainTriggererFactory
import com.willfp.libreforge.triggers.DispatchedTrigger

object CycleTriggererFactory: ChainTriggererFactory("cycle") {
    override fun create() = CycleChainTriggerer()

    class CycleChainTriggerer : ChainTriggerer {
        private var offset = 0

        override fun trigger(chain: Chain, trigger: DispatchedTrigger) {
            offset %= chain.size
            chain[offset].trigger(trigger)
            offset++
        }
    }
}