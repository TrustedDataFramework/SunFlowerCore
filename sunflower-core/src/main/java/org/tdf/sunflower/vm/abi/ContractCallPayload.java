package org.tdf.sunflower.vm.abi;

import lombok.Data;

@Data
public class ContractCallPayload {
    private String method;
    private Parameters parameters;
}
