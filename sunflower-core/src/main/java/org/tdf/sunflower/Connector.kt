package org.tdf.sunflower

import org.springframework.stereotype.Component
import org.tdf.sunflower.p2pv2.client.PeerClient
import javax.annotation.PostConstruct

@Component
class Connector(val cli: PeerClient) {


    @PostConstruct
    fun conn() {
        cli.connectAsync("192.168.1.188", 7010)
    }
}