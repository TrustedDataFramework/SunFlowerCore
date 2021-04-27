import { JsonFragment } from "./abi";
export { link } from "../linker/pkg"


// inline abi into webassembly bytecode
export function inline(code: string, abi: string | JsonFragment): string {
    try {
        // new abi.Interface here to validate abi

    } catch (e) {

    }
    // parse code into module, add custom section which named abi
    return ''
}