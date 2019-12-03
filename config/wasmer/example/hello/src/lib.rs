extern crate wisdom;

use wisdom::log;

#[no_mangle]
pub fn invoke() {
    let s = "hello world!";
    log(&s);
}
