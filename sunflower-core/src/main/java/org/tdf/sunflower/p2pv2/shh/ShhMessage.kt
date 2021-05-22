package org.tdf.sunflower.p2pv2.shh

import org.tdf.sunflower.p2pv2.message.Message

abstract class ShhMessage(command: ShhMessageCodes) : Message(command)