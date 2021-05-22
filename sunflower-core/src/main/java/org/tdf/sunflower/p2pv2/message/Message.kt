package org.tdf.sunflower.p2pv2.message

import org.tdf.sunflower.p2pv2.MessageCode

abstract class Message(val command: MessageCode) {
    val code: Int
        get() = command.code
}


