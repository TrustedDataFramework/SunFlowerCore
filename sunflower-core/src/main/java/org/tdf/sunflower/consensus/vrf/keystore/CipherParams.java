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

public class CipherParams {
    private String iv;

    public CipherParams() {
        this(null);
    }

    public CipherParams(String iv) {
        this.iv = iv;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}