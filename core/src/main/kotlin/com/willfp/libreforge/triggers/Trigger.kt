package com.willfp.libreforge.triggers

import com.sun.tools.javac.jvm.ByteCodes.ret
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.libreforge.BlankHolder.effects
import com.willfp.libreforge.EmptyProvidedHolder.holder
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.generatePlaceholders
import com.willfp.libreforge.getProvidedActiveEffects
import com.willfp.libreforge.plugin
import com.willfp.libreforge.providedActiveEffects
import com.willfp.libreforge.triggers.event.TriggerDispatchEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

abstract class Trigger(
    override val id: String
) : Listener, KRegistrable {
    /**
     * The TriggerData parameters that are sent.
     */
    abstract val parameters: Set<TriggerParameter>

    /**
     * Whether this trigger is enabled.
     */
    open var isEnabled: Boolean = false
        protected set

    /**
     * Enable the trigger.
     */
    fun enable() {
        isEnabled = true
    }

    /**
     * Dispatch the trigger.
     */
    protected fun dispatch(
        player: Player,
        data: TriggerData,
        forceHolders: Collection<ProvidedHolder>? = null
    ) {
        val effects = forceHolders?.getProvidedActiveEffects(player) ?: player.providedActiveEffects

        // First check if the dispatch would ever succeed to avoid unnecessary processing
        if (effects.flatMap { it.effects }.none { it.canBeTriggeredBy(this) }) {
            return
        }

        val dispatch = plugin.dispatchedTriggerFactory.create(player, this, data) ?: return

        dispatch.generatePlaceholders(data)

        val dispatchEvent = TriggerDispatchEvent(player, dispatch)
        Bukkit.getPluginManager().callEvent(dispatchEvent)
        if (dispatchEvent.isCancelled) {
            return
        }

        for ((holder, blocks) in effects) {
            // Check again here to avoid generating placeholders for nothing
            if (blocks.none { it.canBeTriggeredBy(this) }) {
                continue
            }

            val withHolder = data.copy(holder = holder)
            val dispatchWithHolder = DispatchedTrigger(player, this, withHolder).inheritPlaceholders(dispatch)

            for (placeholder in holder.generatePlaceholders(player)) {
                dispatchWithHolder.addPlaceholder(placeholder)
            }

            for (block in blocks) {
                block.tryTrigger(dispatchWithHolder)
            }
        }
    }

    final override fun onRegister() {
        plugin.runWhenEnabled {
            plugin.eventManager.unregisterListener(this)
            plugin.eventManager.registerListener(this)
            postRegister()
        }
    }

    open fun postRegister() {
        // Override when needed.
    }

    override fun getID() = id

    override fun equals(other: Any?): Boolean {
        return other is Trigger && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
