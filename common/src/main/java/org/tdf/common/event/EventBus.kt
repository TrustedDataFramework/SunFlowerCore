package org.tdf.common.event

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * lock-free event bus implementation
 */
class EventBus : CoroutineScope{
    override val coroutineContext: CoroutineContext = CoroutineName("event-bus")
    private val listenersLock = false
    private var listeners: Map<Class<*>, List<Consumer<Any>>> = HashMap()

    /**
     * subscribe a listener to event
     *
     * @param eventType type of event
     * @param listener  listener which applied when some event published
     * @param <T>       generic
    </T> */
    fun <T> subscribe(eventType: Class<T>, listener: Consumer<in T>) {
        synchronized(listenersLock) {

            // copy when write, avoid concurrent modifications
            val copied = copy(listeners)
            copied.putIfAbsent(eventType, ArrayList())
            copied[eventType]!!.add(
                listener as Consumer<Any>
            )
            listeners = copied
        }
    }

    /**
     * publish a event to listeners
     *
     * @param event the event to publish
     */
    fun publish(event: Any) {
        val consumers: List<Consumer<Any>> = listeners.getOrDefault(event.javaClass, emptyList())
        for (consumer in consumers) {
            launch {
                try {
                    consumer.accept(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun copy(listeners: Map<Class<*>, List<Consumer<Any>>>): MutableMap<Class<*>, MutableList<Consumer<Any>>> {
        val ret: MutableMap<Class<*>, MutableList<Consumer<Any>>> = HashMap()
        listeners.forEach { (k: Class<*>, v: List<Consumer<Any>>?) -> ret[k] = ArrayList(v) }
        return ret
    }
}