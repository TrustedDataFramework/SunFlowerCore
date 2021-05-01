package org.tdf.sunflower.vm.abi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Parameters;
import org.tdf.common.util.HexBytes;
import org.tdf.sunflower.vm.ContractABI;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDeployPayload {
    // 合约字节码
    private HexBytes binary;
    // 构造器参数
    private Parameters parameters;
    // 合约 abi
    private List<ContractABI> contractABIs;
}
