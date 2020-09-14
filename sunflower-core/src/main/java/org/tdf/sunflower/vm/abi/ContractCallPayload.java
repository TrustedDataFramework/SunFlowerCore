package org.tdf.sunflower.vm.abi;

import lombok.Data;
import org.tdf.common.types.Parameters;

@Data
public class ContractCallPayload {
    private String method;
    private Parameters parameters;
}
