# Hello

A smart contract example

## Build

- ensure you wasm-gc is installed

```shell script
cargo install wasm-gc
```

```shell script
RUSTFLAGS="-C link-arg=-zstack-size=32768" cargo build --release --target=wasm32-unknown-unknown # linux shell only 
# we not need default 1M stack size, 32768 is enough
wasm-gc hello.wasm hello.wasm # use wasm-gc to reduce file size
```
