#![no_std]

extern "C"{
    pub fn _log(content: *const u8, len: usize);
}

pub fn log(msg: &str) {
    unsafe {
        let msg = msg.as_bytes();
        _log(msg.as_ptr(), msg.len());
    }
}