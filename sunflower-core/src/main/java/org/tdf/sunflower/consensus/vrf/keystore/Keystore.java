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

import java.nio.file.Path;

import org.tdf.crypto.PrivateKey;


/**
 * Keystore interface definition
 *
 * Each method could throw {RuntimeException}, because of access to IO and crypto functions.
 *
 * @author James Hu
 * @since 2019/6/15
 */
public interface Keystore {

    void removeKey(String pubkey);

    void storeKey(PrivateKey key, String password) throws RuntimeException;

    void storeRawKeystore(String content, String pubkey) throws RuntimeException;

    String[] listStoredKeys();

    PrivateKey loadStoredKey(String pubkey, String password) throws RuntimeException;

    /**
     * Check if keystore has file with key for passed pubkey.
     * @param pubkey - 64 chars
     * @return
     */
    boolean hasStoredKey(String pubkey);

    Path getKeyStoreLocation();
}
