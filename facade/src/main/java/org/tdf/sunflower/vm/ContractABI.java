package org.tdf.sunflower.vm;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
public class ContractABI {
    private String name;
    // 0 = function 1 = event
    private int type;
    private int[] inputs;
    private int[] outputs;

    public ContractABIJson toJSON() {
        return new ContractABIJson(
            name,
            type == 0 ? "function" : "event",
            map(inputs),
            map(outputs)
        );
    }

    private List<InputOutput> map(int[] ints) {
        return Collections.emptyList();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InputOutput {
        private String type;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContractABIJson {
        private String name;
        private String type;
        private List<InputOutput> inputs;
        private List<InputOutput> outputs;
    }
}
