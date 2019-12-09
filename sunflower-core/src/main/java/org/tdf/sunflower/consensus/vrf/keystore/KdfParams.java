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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class KdfParams {
    private Integer c;
    private Integer dklen;
    private String salt;
    private Integer n;
    private Integer p;
    private Integer r;


    public Integer getN() {
        return n;
    }

    public Integer getR() {
        return r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public Integer getP() {
        return p;
    }

    public void setP(Integer p) {
        this.p = p;
    }

    @JsonIgnore
    public Integer getC() {
        return c;
    }

    public void setC(Integer c) {
        this.c = c;
    }

    public Integer getDklen() {
        return dklen;
    }

    public void setDklen(Integer dklen) {
        this.dklen = dklen;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
