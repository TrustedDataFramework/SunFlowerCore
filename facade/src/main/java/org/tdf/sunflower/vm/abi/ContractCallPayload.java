package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Parameters;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractCallPayload {
    private String method;
    private Parameters parameters;
}
