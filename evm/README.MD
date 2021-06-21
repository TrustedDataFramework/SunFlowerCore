# Ethereum Virtual Machine(EVM)

Evm Opcode pseudo code

读写越界溢出的处理

- 栈越界或者溢出，合约报错，终止
- 内存溢出，合约报错终止
- code, calldata 允许溢出
- 允许 divisor = 0

1. Stop(0x00)

return empty

2. Add(0x01), Mul(0x02), Sub(0x03), Div(0x04), SDiv(0x05), Mod(0x06), SMod(0x07), Exp(0x0a), Lt(0x10), GT(0x11), SLT(
   0x12), SGT(0x13), EQ(0x14), And(0x16), Or(0x17), Xor(0x18), Shl(0x1b), Shr(0x1c), Sar(0x1d)

```go
left, right := stack.pop(), stack.pop()

switch(op) {
    case Div, SDiv, Mod, SMOD :{
        if right == 0 {
            stack.push(0)
        } else {
            stack.push(op(left, right))
        }
    }
    default: {
        stack.push(op(left, right))
    }
}
```

3. AddMod(0x08), MulMod(0x09)

```go
x, y, z = stack.pop(), stack.pop(), stack.pop()
if z == 0 {
    stack.push(0)
} else {
    stack.push(op(x, y) % z)
}
```

4. SignExtend(0x0b)

```go
base, n = stack.pop(), stack.pop()

if n >= 32 {
    stack.push(base)
} else {
    if (base[32 - n] & 0x80) ==0 {
        stack.push(base[32 - n:32])
    } else {
        copy(base[:32-n], (uint256(-1))[:32-n])
        stack.push(base)
    }
}
```

5. IsZero(0x15), Not(0x16)

```go
stack.push(op(stack.pop()))
```

6. Byte(0x1a)

```go
n, value := stack.pop(), stack.pop()
if n >= 32 {
    stack.push(0)
} else {
    stack.push(value[n:n+1])
}
```

7. Sha3(0x20)

```go
off, size := stack.pop(), stack.pop()
if off + size > limit {
    panic("memory overflow")
}
stack.push(sha3(memory[off:off+size]))
```

8. Address(0x30)

```go
var data byte[32]
copy(data[32-20:32], contract.Address())
stack.push(data)
```

9. Balance(0x31)

```go
stack.push(backend.GetBalance(stack.pop()[32-20:]))
```

10. Origin(0x32)

```go
var data byte[32]
copy(data[32-20:32], tx.SenderAddress())
stack.push(data)
```

11. Caller(0x33)

```go
var data byte[32]
copy(data[32-20:32], contract.Caller)
stack.push(data)
```

12. CallValue(0x34)

```go
stack.push(contract.CallValue())
```

13. CallDataLoad(0x35)

```go
offset := stack.pop() & 0xffffffff

if offset > len(calldata) {
    offset = len(calldata)
}

end := offset + 32

if end > len(calldata) {
    end = len(calldata)
}

length := end - start
var data byte[32]
copy(data[32-length:32], calldata[offset:end])
stack.push(data)
```

14. CallDataSize(0x36)

```go
stack.push(len(calldata))
```

15. CallDataCopy(0x37)

```go
memOff, dataOff, len := 
    stack.pop() & 0xffffffff, 
    stack.pop() & 0xffffffff, 
    stack.pop() & 0xffffffff

if dataOff > len(calldata) {
    dataOff = len(calldata)
}

end := dataOff + len


```

16. CodeSize(0x38)

```go
stack.push(len(code))
```

17. CodeCopy(0x39)

```go
memOff, codeOff, len := 
    stack.pop() & 0xffffffff, 
    stack.pop() & 0xffffffff, 
    stack.pop() & 0xffffffff

copy(mem[memOff: memOff+len], code[codeOff: codeOff+len]
```

18. ReturnDataSize(0x3d)
19. ReturnDataCopy(0x3e)
20. GasPrice(0x3a)
21. ExtCodeCopy(0x3c)
22. ExtCodeHash(0x3f)