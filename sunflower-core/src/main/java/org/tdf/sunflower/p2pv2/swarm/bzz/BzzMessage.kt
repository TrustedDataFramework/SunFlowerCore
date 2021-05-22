package org.tdf.sunflower.p2pv2.swarm.bzz

import org.tdf.sunflower.p2pv2.message.Message

abstract class BzzMessage(command: BzzMessageCodes) : Message(command)