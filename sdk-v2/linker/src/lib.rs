mod utils;

use wasm_bindgen::prelude::*;

// When the `wee_alloc` feature is enabled, use `wee_alloc` as the global
// allocator.
#[cfg(feature = "wee_alloc")]
#[global_allocator]
static ALLOC: wee_alloc::WeeAlloc = wee_alloc::WeeAlloc::INIT;
use hex::{FromHex, ToHex};
use parity_wasm::elements::{CustomSection, Deserialize, Module, Section, Serialize, deserialize_buffer};

#[wasm_bindgen]
extern {
    fn alert(s: &str);
}

fn drop_blank(s: &str) -> String{
    // drop blank
    let mut ret = String::new();
    for c in s.chars().into_iter() {
        if c == ' ' || c == '\n' || c == ' ' || c == '\t' {
            continue;
        }
        ret.push(c);
    }    
    ret
}

fn decode_hex(s: &str) -> Result<Vec<u8>, String> {
    // drop blank
    let s = drop_blank(s);
    let mut out: Vec<u8> = vec![0u8; s.len() / 2];
    hex::decode_to_slice(&s, &mut out).map_err(|_| "decode hex failed".to_string())?;
    Ok(out)
}


#[wasm_bindgen]
/// link abi into wasm byte code 
pub fn link(code: String, abi: String, init: String) -> String {
    let c = decode_hex(&code).unwrap();
    let mut m: Module = deserialize_buffer(&c).unwrap();
    let linked = m.custom_sections().any(|x| x.name() == "__abi");

    if !linked {
        let new_section = Section::Custom(CustomSection::new("__abi".to_string(), abi.into_bytes()));
        m.sections_mut().insert(0, new_section);
    }

    let linked = m.custom_sections().any(|x| x.name() == "__init");

    if !linked {
        let data = decode_hex(&init).unwrap();
        let new_section = Section::Custom(CustomSection::new("__init".to_string(), data));
        m.sections_mut().insert(0, new_section);        
    }
  
    let mut v = Vec::new();
    m.serialize(&mut v).unwrap();
    v.encode_hex()
}
