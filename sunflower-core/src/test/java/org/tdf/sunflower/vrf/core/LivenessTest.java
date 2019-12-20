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
package org.tdf.sunflower.vrf.core;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tdf.sunflower.consensus.vrf.core.Liveness;
import org.tdf.sunflower.consensus.vrf.core.Validator;

public class LivenessTest {

    private static final int[] HIT_INDEX_ARRAY = new int[] { 21, 34, 98, 108, 182, 290, 312, 377, 492, 592, 601, 669,
            698, 713, 823, 872, 999 };

    private static final int[] PRIORITY_ARRAY = new int[] { 1, 2, 1, 1, 1, 2, 3, 1, 3, 1, 1, 2, 2, 2, 1, 3, 1 };

    private static final byte[] coinbase = Hex.decode("3a0b32b4e6f404934d098957d200e803239fdf75");

    @Test
    public void testLiveness() {
        Liveness liveness = new Liveness(coinbase, 0, 1);

        int hits = 0;
        for (int i = 0; i < HIT_INDEX_ARRAY.length; ++i) {
            liveness.reset(HIT_INDEX_ARRAY[i], PRIORITY_ARRAY[i]);
            hits += PRIORITY_ARRAY[i];
        }

        assertTrue(liveness.getMisses() == 0);
        assertTrue(liveness.getIndex() == 999);

        int misses = liveness.stampMisses(1000, Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE * 3);

        assertTrue(misses == 0);
    }

    @Test
    public void testOverflow() {
        long index = Long.MAX_VALUE - 3;

        Liveness liveness = new Liveness(coinbase, index, 1);
        for (int i = 0; i < 100; ++i) {
            ++index;
            if (index < 0)
                index = 0;

            liveness.activate(index, 1);
        }

        int misses = liveness.stampMisses(index, Validator.DEPOSIT_MIN_VALUE, Validator.DEPOSIT_MIN_VALUE);

        assertTrue(liveness.getMisses() == 0);
    }

    @Test
    public void testHit1() {
        final long loop = 10000;

        long index = Long.MAX_VALUE - 3;

        long weight = Validator.DEPOSIT_MIN_VALUE;
        long totalWeight = Validator.DEPOSIT_MIN_VALUE * 3;

        long hitCycle = Liveness.getActiveCycle(weight, totalWeight);

        Liveness liveness = new Liveness(coinbase, index, 1);
        for (int i = 0; i < loop; ++i) {
            ++index;
            if (index < 0)
                index = 0;

            if (i % 3 == 0) {
                liveness.activate(index, 1);
            }

            // Try to get hit misses every hit cycle
            if (i % hitCycle == 0) {
                int misses = liveness.stampMisses(index, weight, totalWeight);

                assertTrue(misses == 0);
            }

            // Try to reset liveness every 3 hit cycle
            long hitSpan = liveness.getLiveSpan(index);
            if (hitSpan > 3 * hitCycle) {
                liveness.reset(index, 1);
            }
        }
        assertTrue(liveness.getMisses() == 0);
    }

    @Test
    public void testHit2() {
        final long loop = 10000;

        long index = Long.MAX_VALUE - 3;

        long weight = Validator.DEPOSIT_MIN_VALUE;
        long totalWeight = Validator.DEPOSIT_MIN_VALUE * 3;

        long hitCycle = Liveness.getActiveCycle(weight, totalWeight);

        Liveness liveness = new Liveness(coinbase, index, 1);
        for (int i = 0; i < loop; ++i) {
            ++index;
            if (index < 0)
                index = 0;

            if (i % 3 == 0) {
                liveness.activate(index, 1);
            }

            // Try to get hit misses every hit cycle
            if (i % hitCycle == 0) {
                int misses = liveness.stampMisses(index, weight, totalWeight);

                assertTrue(misses == 0);
            }
        }
        assertTrue(liveness.getMisses() == 0);
    }

    @Test
    public void testMiss1() {
        final long loop = 10000;

        long index = Long.MAX_VALUE - 3;

        long weight = Validator.DEPOSIT_MIN_VALUE;
        long totalWeight = Validator.DEPOSIT_MIN_VALUE * 3;

        long hitCycle = Liveness.getActiveCycle(weight, totalWeight);

        Liveness liveness = new Liveness(coinbase, index, 1);
        for (int i = 0; i < loop; ++i) {
            ++index;
            if (index < 0)
                index = 0;

            // Try to get hit misses every hit cycle
            if (i % hitCycle == 0) {
                int misses = liveness.stampMisses(index, weight, totalWeight);

                if (i >= hitCycle * 2) {
                    assertTrue(liveness.getMisses() > 0);
                }
                // System.out.println("misses = " + misses);
            }
        }
        assertTrue(liveness.getMisses() > 0);
    }

    @Test
    public void testMiss2() {
        final long loop = 10000;

        long index = Long.MAX_VALUE - 3;

        long weight = Validator.DEPOSIT_MIN_VALUE;
        long totalWeight = Validator.DEPOSIT_MIN_VALUE * 3;

        long hitCycle = Liveness.getActiveCycle(weight, totalWeight);

        Liveness liveness = new Liveness(coinbase, index, 1);
        for (int i = 0; i < loop; ++i) {
            ++index;
            if (index < 0)
                index = 0;

            if (i % 4 == 0) {
                liveness.activate(index, 1);
            }

            // Try to get hit misses every hit cycle
            if (i % hitCycle == 0) {
                int misses = liveness.stampMisses(index, weight, totalWeight);

                if (i >= hitCycle * 2) {
                    // assertTrue(liveness.getMisses() > 0);
                }
                System.out.println("misses = " + misses);
            }
        }
        assertTrue(liveness.getMisses() > 0);
    }
}