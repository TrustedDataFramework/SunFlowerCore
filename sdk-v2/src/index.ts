
export { link } from "../linker/pkg"
export { compileRust } from './abi'

import { link } from "../linker/pkg"
import { JsonFragment } from '@ethersproject/abi'
import { utils } from 'ethers'

// inline abi and constructor arguments into webassembly bytecode
export function inline(code: utils.BytesLike, abi: string | JsonFragment[], args: any[]): string {
    if (!utils.isBytesLike(code)) {
        throw new Error('inline failed: code is not bytes like')
    }
    
    // new abi.Interface here to validate abi
    if (typeof abi !== 'string')
        abi = JSON.stringify(abi, null, 0)    
    let encoder = new utils.Interface(<any> abi)
    let encoded = encoder.encodeDeploy(args)

    // link abi as custom section into module
    // and encoded as rlp
    return '0x' + link(utils.hexlify(code).substring(2), <any> abi, encoded.substring(2))
}
