package org.tdf.sunflower.vm.abi;

import lombok.Data;
import org.tdf.rlp.RLPElement;

@Data
public class Parameters {
    // 参数类型
    private long[] types;
    // 具体的参数
    private RLPElement li;
    // 返回类型，目前近支持单个参数返回
    private int[] returnType;
}
