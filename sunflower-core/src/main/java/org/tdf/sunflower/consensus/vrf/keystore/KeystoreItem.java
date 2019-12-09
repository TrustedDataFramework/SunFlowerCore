/*
 * Copyright (c) [2019] [ <silk chain> ]
 * This file is part of the silk chain library.
 *
 * The silk chain library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The silk chain library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the silk chain library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tdf.sunflower.consensus.vrf.keystore;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * @author James Hu
 * @since 2019/6/15
 */
public class KeystoreItem {

    public KeystoreCrypto crypto;
    public String id;
    public Integer version;
    public String pubkey;

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public KeystoreCrypto getCrypto() {
        return crypto;
    }

    @JsonSetter("crypto")
    public void setCrypto(KeystoreCrypto crypto) {
        this.crypto = crypto;
    }

    @JsonSetter("Crypto")
    //public void setCryptoOld(KeystoreCrypto crypto) {
    //    this.crypto = crypto;
    //}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}