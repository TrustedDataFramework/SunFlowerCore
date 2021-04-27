#!/usr/bin/env node -r ts-node/register
export interface JsonFragmentType {
    readonly name?: string;
    readonly type: 'string' | 'uint' | 'uint64' | 'uint256' | 'address' | 'bool' | 'bytes' | 'int64' 
    | 'string[]' | 'uint64[]' | 'uint256[]' | 'uint[]' | 'address[]' | 'bool[]' | 'bytes[]' | 'int64[]';
}

export interface JsonFragment {
    readonly name?: string;
    readonly type: 'function' | 'event' | 'constructor'; // function or event


    readonly inputs: ReadonlyArray<JsonFragmentType>;
    readonly outputs: ReadonlyArray<JsonFragmentType>;
    readonly stateMutability: 'payable' | 'pure' | 'view' | 'nonpayable'
};



function compileRust(str: string): JsonFragment[] {
  let re = /#\[no_mangle\][\s\n\t]*pub[\s\n\t]+fn[\s\n\t]+([a-zA-Z_][a-zA-Z0-9_]*)[\s\n\t]*\(([a-z\n\s\tA-Z0-9_,:]*)\)[\s\n\t]*->[\s\n\t]*(.*){/g
  const ret = []
  const types = {
    u64: 'u64',
    i64: 'i64',
    bool: 'bool',
    string: 'string',
    'Vec<u8>': 'bytes',
    Address: 'address',
    U256: 'uint256',
    String: 'string',
    'boolean': 'bool',
  }   

  function getInputs(str: string): JsonFragmentType[] {
    const ret = []
    for(let p of str.split(',')) {
      if(!p) continue
      const lr = p.split(':')
      ret.push({
        name: lr[0].trim(),
        type: types[lr[1].trim()]
      })
    }

    return ret
  }

  function getOutputs(str: string): JsonFragmentType[] {
    for(let t of Object.keys(types)) {
      if (str.indexOf(t) >= 0) 
        return [{type: types[t]}]
    }
    return []
  }  

  for (let m of str.match(re) || []) {
    re.lastIndex = 0
    const r = re.exec(m)
    ret.push({
      name: r[1],
      type: 'function',
      inputs: getInputs(r[2]),
      outputs: getOutputs(r[3]),
      stateMutability: 'payable'
    })
  }  

  return ret
}
