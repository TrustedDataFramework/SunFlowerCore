package org.tdf.sunflower.p2pv2

class MessageRoundtrip (val msg: Message)
{
    var lastTimestamp: Long = 0L
    var retryTimes: Long = 0L
    var answered: Boolean = false

    fun answer() {
        this.answered = true
    }

    fun incRetryTimes() {
        ++retryTimes
    }

    fun saveTime() {
        lastTimestamp = System.currentTimeMillis()
    }

    fun hasToRetry(): Boolean {
        return 20000 < System.currentTimeMillis() - lastTimestamp
    }
}