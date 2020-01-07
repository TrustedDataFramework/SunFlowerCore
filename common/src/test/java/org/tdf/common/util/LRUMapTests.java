package org.tdf.common.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;


@RunWith(JUnit4.class)
public class LRUMapTests {
    private LRUMap<String, String> lruMap;

    private List<String> evicted;

    @Before
    public void before() {
        evicted = new ArrayList<>();
        lruMap = LRUMap
                .<String, String>builder()
                .maximumSize(2)
                .hook((k, v) -> evicted.add(k))
                .build();
        lruMap.put("1", "2");
        lruMap.put("2", "3");
    }

    @Test
    public void test() {
        lruMap.put("3", "4");
        assert evicted.size() == 1;
        assert evicted.get(0).equals("1");

        lruMap.put("4", "5");
        assert evicted.size() == 2;
        assert evicted.get(0).equals("1");
        assert evicted.get(1).equals("2");
    }
}
