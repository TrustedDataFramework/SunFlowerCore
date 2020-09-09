package org.tdf.sunflower.vm.abi;

public class ContractABI {
    private String name;
    // 0 = function 1 = event
    private int type;
    private int[] inputs;
    private int[] outputs;
}
