# TDS 智能合约编写指引 (AssemblyScript 篇)

[TOC]



## AssemblyScript 简介


AssemblyScript 是 TypeScript 的一个变种，和 TypeScript 不同，AssemblyScript 使用严格类型。


## AssemblyScript 基础类型

### AssemblyScript 每个变量的类型是不可变的。AssembyScript 中的类型分为两种，一种是基本类型，另一种是引用类型。AssemblyScript 的所有基本类型列举如下：

| AssemblyScript 类型 | WebAssembly 类型 | 描述              |
| ------------------- | ---------------- | ----------------- |
| ```i32```           | ```i32```        | 32 bit 有符号整数 |
| ```u32```           | ```i32```        | 32 bit 无符号整数 |
| ```i64```           | ```i64```        | 64 bit 有符号整数 |
| ```u64```           | ```i64```        | 64 bit 无符号整数 |
| ```f32```           | ```f32```        | 单精度浮点数      |
| ```f64```           | ```f64```        | 双精度浮点数      |
| ```i8```            | ```i32```        | 8 bit 有符号整数  |
| ```u8```            | ```i32```        | 8 bit 无符号整数  |
| ```i16```           | ```i32```        | 16 bit 有符号整数 |
| ```u16```          | ```i32```        | 16 bit 无符号整数            |
| ```bool```          | ```i32```        | 布尔型            |


除了以上表中的基本类型以外的其他类型都是引用类型。


### 类型转换

当 AssemblyScript 编译器检查到存在可能不兼容的隐式类型转换时，编译会以异常结果终止。如果需要进行可能不兼容的类型转换，请使用强制类型转换。

在AssemblyScript中，以上提到的每一个类型都有对应的强制转换函数。例如将一个 64 bit 无符号整数 类型的整数强制转换为 32 bit 无符号整数：

```typescript
const i: u64 = 123456789;
const j = u64(i);
```

### 类型声明

AssemblyScript编译器必须在编译时知道每个表达式的类型。这意味着变量和参数在声明时必须同时声明其类型。如果没有声明类型，编译器将首先假定类型为```i32```，在数值过大时再考虑 ```i64```，如果是浮点数就是用 ```f64```。如果变量是其他函数的返回值，则变量的类型是函数返回值的类型。此外，所有函数的返回值类型都必须事先声明，以帮助编译器类型推断。

合法的函数：

```typescript
function sayHello(): void{
    log("hello world");
}
```

语法不正确的函数：


```typescript
function sayHello(): { // 缺少类型声明 sayHello(): void
    log("hello world");
}
```

### 空值

许多编程语言具有一个特殊的 ```null``` 类型表示空值，例如 javascript 和 java 的 ```null```, go 语言和 python 的 ```nil```。事实上 ```null``` 类型的引入给程序带来了许多不可预知性，空值检查的遗漏会给智能合约带来安全隐患，因此 TDS 智能合约的编写没有引入 ```null``` 类型。


### 类型转换兼容性

在下表中，列出了所有基本类型的转换兼容性，打勾向表示从左右到右可以进行隐式的类型转换。





| ↱                   | ```bool``` | ```i8```/```u8``` | ```i16```/```u16``` | ```i32```/```u32``` | ```i64```/```u64``` | ```f32``` | ```f64``` |
| ------------------- | ---------- | ----------------- | ------------------- | ------------------- | ------------------- | --------- | --------- |
| ```bool```          | ✓          | ✓                 | ✓                   | ✓                   | ✓                   | ✓         | ✓         |
| ```i8```/```u8```   |            | ✓                 | ✓                   | ✓                   | ✓                   | ✓         | ✓         |
| ```i16```/```u16``` |            |                   | ✓                   | ✓                   | ✓                   | ✓         | ✓         |
| ```i32```/```u32``` |            |                   |                     | ✓                   | ✓                   |           | ✓         |
| ```i64```/```u64``` |            |                   |                     |                     | ✓                   |           |           |
| ```f32```           |            |                   |                     |                     |                     | ✓         | ✓         |
| ```f64```           |            |                   |                     |                     |                     |           | ✓         |


### 数值比较

当使用比较运算符 ```!=``` 和 ```==``` 时，如果两个数值在类型转换时是兼容的，则不需要强制类型转换就可以进行比较。

操作符 ```>```，```<```，```>=```，```<=``` 对无符号整数和有符号整数有不同的比较方式，被比较的两个数值要么都是有符号整数，要么都是无符号整数，且具有转换兼容性。


### 移位操作

移位操作符 ```<<```，```>>``` 的结果类型是操作符左端的类型，右端类型会被隐式转换成左端的类型。如果左端类型是有符号整数，执行算术移位，如果左端是无符号整数，则执行逻辑移位。

无符号右移操作符 ```>>>``` 类似，但始终执行逻辑移位。

## 模块化

一个 AssemblyScript 智能合约项目可能由多个文件组成，文件与文件之间可以存在互相引用的关系，互相使用对方导出的内容。。AssemblyScript 项目编译成 wasm 字节码时，需要指定一个入口文件，只有这个入口文件中被导出的函数才可以在将来被调用到。

### 函数导出


```typescript
export function add(a: i32, b: i32): i32 {
  return a + b
}
```


### 全局变量导出

```typescript
export const foo = 1
export var bar = 2
```


### 类导出

```typescript
export class Bar {
    a: i32 = 1
    getA(): i32 { return this.a }
}
```

### 导入

若建立以下多文件项目，指定 ```index.ts``` 为编译时的入口文件

```sh
indext.ts
foo.ts
```

在 foo.ts 文件中包含了以下内容：

```typescript
export function add(a: i32, b: i32): i32{
    return a + b;
}
```


在 index.ts 中导入 ```add``` 函数：


```typescript
import {add} from './foo.ts'

function addOne(a: i32): i32{
    return add(a, 1);
}
```

## 标准库


### 全局变量

| 变量名         | 类型                     | 描述                                    |
| -------------- | ------------------------ | --------------------------------------- |
| ```NaN```      | ```f32``` 或者 ```f64``` | not a number，表示不是一个有效的浮点数  |
| ```Infinity``` | ```f32``` 或者 ```f64``` | 表示无穷大   ```-Infinity``` 表示无穷小 |


### 全局函数

| 函数名     | 参数个数 | 参数列表               | 返回值类型 | 描述                                                         |
| ---------- | -------- | ---------------------- | ---------- | ------------------------------------------------------------ |
| ```isNaN``` | 1        | ```f32``` 或 ```f64``` | ```bool``` | 判断一个浮点数是否无效                                       |
| ```isFinite``` | 1        | ```f32``` 或```f64``` | ```bool``` | 判断一个浮点数满足：1. 不是无穷大 2. 不是无穷小 3. 有效      |
| ```parseInt``` | 1 或 2 | ```(string, radisx?: i32)``` | ```i64```  | 从字符串解析成一个整数，```radix```等于10则使用 10 进制，默认 ```radix``` 是 10 |
| ```parseFloat``` | 1        | ```(string)```         | ```f64```  | 从字符串解析成一个浮点数，使用10进制                         |

### 数组（Array）

AssemblyScript 中的 ```Array<T>``` 与 JavaScript 中的 Array 非常相似。区别在于除了基本类型以外的数组初始化后，数组中的元素必须显示初始化后才可以访问。例如：


1. 使用基本类型初始化：

```typescript
const arr = new Array<u64>(10); // 使用基本类型 u64 创建数组
const zero = arr[0]; // zero 的值是 0，类型是 u64
```

2. 使用引用类型初始化：

```typescript
const arr = new Array<string>(10); // 使用基本类型 u64 创建数组
const zero = arr[0]; // 因为 TDS 合约不允许 null 值，所以这里会报错，因为 arr[0] 没有被初始化

// 正确的做法是进行初始化
for(let i = 0; i < arr.length; i++){
    arr[i] = "";
}
```


3. ```Array<T>``` 类常用的成员：

| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```new``` | 构造器 | 0或1  | ```i32``` |      ```Array<T>```     |  ```new Array<i32>(1)```    |  构造器    |
|  ```isArray```  | 静态函数       |  1        |     任意    |  ```bool```    |  ```Array.isArray(arr)```   |   判断一个变量是否是数组   |
|  ```length```  |   字段     |    -      |    -      |     ```i32```     | ```arr.length``` | 数组的长度 |
| ```concat``` | 方法 | 1 | ```Array<T>``` | ```Array<T>``` | ```arr0.concat(arr1)``` | 把两个数组拼接成一个数组 |
| ```every``` | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```bool``` | ```arr.every(fn)``` | 判断数组的每个元素是否都满足```fn``` |
| ```fill``` | 方法| 1、2或3 | ```(value: T, start?: i32, end?: i32)``` | 返回自身 | ```arr.fill(0, 0, arr.length)``` | 对数组用```value```进行填充，```start```和```end```分别是填充的起始索引（包含）和结束索引（不包含） |
| ```filter``` | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```Array<T>``` | ```arr.filter(fn)``` | 过滤掉数组中不符合```fn```的元素 |
| ```findIndex``` | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```i32``` | ```arr.findIndex(fn)``` | 获取到第一个满足```fn```的元素所在的索引或者```-1``` |
| ```forEach``` | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => void``` | ```void``` | ```arr.forEach(fn)``` | 用```fn```遍历数组 |
| ```includes``` | 方法| 1或2 | ```(value: T, fromIndex?: i32)``` | ```bool``` | ```arr.includes(1,0)``` | 判断数组是否包含```value``` |
| ```indexOf``` | 方法| 1或2 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```bool``` | - | 数组的每个元素是否都满足```fn``` |
| ```join``` | 方法| 1 | ```(sep: string)``` | ```string``` | ```arr.join(',')``` | 对数组中每个字符串用字符```sep``` 连接|
| ```lastIndexOf``` | 方法| 1或2 | ```(value: T, fromIndex?: i32)``` | ```i32``` | ```arr.lastIndexOf('.')``` | 获取到最后等于```value```的元素所在的索引或者```-1``` |
| ```map``` | 方法| 1 | ```(fn: (value: T, index: i32, array: Array<T>) => U)``` | ```Array<U>``` | ```arr.map(fn)``` | 把数组```arr``` 的元素作为函数 ```fn``` 的参数映射出新数组 |
| ```pop``` | 方法| 0 | - | ```T``` | ```arr.pop()``` | 弹出数组的最后一个元素 |
| ```push``` | 方法| 1 | ```(value: T)``` | ```i32``` | ```arr.push(1)``` | 向数组尾部增加一个元素，返回数组长度|
| ```reduce``` | 方法| 1或者2| ```(fn: (acc: U, cur: T, idx: i32, src: Array) => U, initialValue: U)``` | ```U``` | ```arr.reduce(fn, 0)``` | 从左端开始对数组进行累加操作，经常和 ```map``` 配合使用|
| ```reduceRight``` | 方法| 1或者2| ```(fn: (acc: U, cur: T, idx: i32, src: Array) => U, initialValue: U)``` | ```U``` | ```arr.reduceRight(fn, 0)``` | 从右端开始对数组进行累加操作|
| ```reverse``` | 方法| 0| - | 返回自身 | ``arr.reverse()`` | 把数组倒过来|
| ```shift``` | 方法| 0| - | ```T``` | ```arr.shift()``` | 弹出数组的第一个元素|
| ```slice``` | 方法| 0、1或2 | ```(start?: i32, end?: i32)``` | ```Array<T>``` | ```arr.slice(0, arr.length)``` | 从数组的```start```（包含）截取到```end```（不包含）|
| ```some``` | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```bool``` | ```arr.some(fn)``` | 判断数组中是否存在至少一个元素满足 ```fn```|
| ```sort``` | 方法 | 0 或 1 | ```fn?: (a: T, b: T) => i32``` | 返回自身 | ```arr.sort(fn)``` | 对数组进行排序，可以传入比较函数 ```fn``` |
| ```splice``` | 方法 | 1 或 2 | ```(start: i32, deleteCount?: i32)``` | ```Array<T>``` | ```arr.splice(1, 2)``` | 从数组中见截断一部分，```start``` 表示开始截断的位置，```deleteCount``` 表示截断掉多少个 |
| ```unshift``` | 方法 | 1 | ```(value: T)``` | ```i32``` | ```arr.unshift(el)``` | 在数组左端添加一个元素|


### string

string 内部是固定长度的UTF-16编码的字节串。AssemblyScript 中 string 的工作原理与JavaScript 中的 string 非常相似。

1. ```string``` 类常用的成员：

| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```charAt``` | 方法 | 1  | ```i32``` |      ```(pos: i32)```     |  ```str.charAt(0)```  |  根据索引查找第 ```pos``` 个 utf16 单元   |
|  ```charCodeAt```  | 方法       |  1        |     任意    |  ```i32```  |  ```str.charCodeAt(0)```  |   根据索引查找第 ```pos``` 个 utf16 单元   |
|  ```length```  |   字段     |    -      |    -      |     ```i32```     | ```str.length``` | 字符串的长度 |
| ```concat``` | 方法 | 1 | ```string``` | ```string``` | ```str0.concat(str1)``` | 拼接字符串，也可以用加号拼接 |
| ```endsWith``` | 方法| 1或2 | ```(search: string, end?: i32)``` | ```bool``` | ```str.endsWith('suffix')``` | 判断字符串是否以```search```结尾，可以用```end```指定搜索的停止位置 |
| ```includes``` | 方法| 1或2 | ```(search: string, start?: i32)``` | ```bool``` | ```str.includes('some')``` | 判断字符串是否包含```search```,可以用```start```指定搜索的起始位置 |
| ```indexOf``` | 方法| 1或2 | ```(search: string, start?: i32)``` | ```i32``` | ```arr.indexOf('s')``` | 从左向右搜索```search```所在的索引或者```-1``` |
| ```lastIndexOf``` | 方法| 1或2 | ```(search: string, start?: i32)``` | ```i32``` | ```arr.lastIndexOf('s')``` | 从右向左搜索```search```所在的位置或者```-1``` |
| ```padStart``` | 方法| 2 | ```(length: i32, pad:string)``` | ```string``` | ```str.padStart(2, '0')``` | 在字符串左端用```pad```补齐，使字符串长度等于```length``` |
| ```padEnd``` | 方法| 2 | ```(length: i32, pad:string)```                              | ```string``` | ```str.padEnd(2,'0')``` | 在字符串右端用```pad```补齐。使字符串长度等于```length``` |
| ```repeat``` | 方法| 0或1 | ```(count?:i32)``` | ```string``` | ```str.repeat(2)``` | 得到字符串重复```count```次拼接的结果 |
| ```replace``` | 方法| 2 | ```(search: string, replacement: string)``` | ```string``` | ```str.replace('a','b')``` | 把字符串中的第一个找到的```search```替换成```replacement``` |
| ```replaceAll``` | 方法| 2 | ```(search: string, replacement: string)```                  | ```string``` | ```str.replaceAll('a','b')``` | 把字符串中所有的```search```替换成```replacement``` |
| ```slice``` | 方法| 1或2 | ```(start: i32, end?: i32)``` | ```string``` | ```str.slice(1)``` | 字符串切片，```start```起始位（包含），```end```表示结束位（不包含） |
| ```split``` | 方法| 0、1或2 | ```(sep?: string, limit?: i32)``` | ```Array<string>``` | ```str.split(',')``` | 把字符串用分割符```sep```分割，```limit```用于指定最多分割的数量 |
| ```startsWith``` | 方法| 1 | ```(search: string, start?: i32)``` | ```i32``` | ```str.startsWith()``` | 判断字符串是否以```search```开头，可以用```start```指定搜索的起始位置 |


### ArrayBuffer

ArrayBuffer 用于表示一段二进制字节串，对二进制字节串的操作通常使用 DataView 接口

ArrayBuffer 成员如下：


| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```new``` | 构造器 | 1   | ```i32``` |      ```ArrayBuffer```     |  ```new ArrayBuffer(1)```    |  构造器    |
|  ```isView```  | 静态函数       |  1        |     任意    |  ```bool```    |  ```ArrayBuffer.isView```   |   判断一个值是否是 TypedArray 或者 DataView   |
|  ```byteLength```  |   字段     |    -      |    -      |     ```i32```     | ```buf.byteLength``` | 字节串的长度 |
| ```slice``` | 方法 | 0、1 或2 | ```(begin?: i32, end?: i32)``` | ```ArrayBuffer``` | ```buf.slice(0, buf.byteLength)``` | 对字节串作切片操作，```begin``` 包含，```end``` 不包含 |

### DataView 

DataView 提供了对二进制字节串操作的接口

DataView 成员如下：

| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```new``` | 构造器 | 1、2或3 | ```(buffer: ArrayBuffer, byteOffset?: i32, byteLength?: i32)```      |      ```DataView```     |  ```new DataView(buf, 0, buf.byteLength)```    |  构造器    |
| ```buffer``` | 字段      |  -       |     -    |  ```ArrayBuffer```         |  ```view.buffer```  |   二进制字节串   |
|  ```byteLength```  |   字段     |    -      |    -      |     ```i32```     | ```buf.byteLength``` | 字节串的长度 |
| ```byteOffset``` | 字段 | - | - | ```i32``` |```buf.byteOffset```|当前偏移量|
| getFloat32 | 方法 | 1 或 2 | ```(byteOffset: i32, littleEndian?: bool)``` | f32 |```view.getFloat32(0)```|从二进制字节串读取一个单精度浮点数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getFloat64 | 方法 | 1 或 2 | ```(byteOffset: i32, littleEndian?: bool)``` | f64 |```view.getFloat64(0)```|从二进制字节串读取一个双精度浮点数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getInt8 | 方法 | 1 | ```byteOffset: i32``` | i8 |```view.getInt8(0)```|从二进制字节串读取一个8bi t有符号整数|
| getInt16 | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | i16 |```view.getInt16(0)```|从二进制字节串读取一个16bit有符号整数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getInt32 | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | i32 |```view.getInt32(0)```|从二进制字节串读取一个32bit有符号整数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getInt64 | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | i64 |```view.getInt64(0)```|从二进制字节串读取一个64bit有符号整数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getUint8 | 方法 | 1 | ```byteOffset: i32``` | u8 |```view.getUint8(0)```|从二进制字节串读取一个8bit无符号整|
| getUint16 | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | i16 |```view.getUint16(0)```|从二进制字节串读取一个16bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| getUint32 | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | u32 |```view.getUint32(0)```|从二进制字节串读取一个32bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为true使用小端编码|
| ```getUint64``` | 方法 | 1或2 | ```(byteOffset: i32, littleEndian?: bool)``` | ```u64``` |```view.getUint64(0)```|从二进制字节串读取一个64bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setFloat32``` | 方法 | 2或3 | ```(byteOffset: i32, value: f32, littleEndian?: bool)``` | ```void``` |```view.setFloat32(0,1.0)```|向二进制字符串放入一个单精度浮点数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setFloat64``` | 方法 | 2或3 | ```(byteOffset: i32, value: f64, littleEndian?: bool)``` | ```void``` |```view.setFloat64(0,1.0)```|向二进制字符串放入一个双精度浮点数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setInt8``` | 方法 | 2 | ```(byteOffset: i32, value: i8)``` | ```void``` |```view.setInt8(0,8)```|向二进制字符串放入一个8bit有符号整数|
| ```setInt16``` | 方法 | 2 | ```(byteOffset: i32, value: i16,littleEndian?: bool)``` | ```void``` |```view.setInt16(0,8)```|向二进制字符串放入一个16bit有符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setInt32``` | 方法 | 2 | ```(byteOffset: i32, value: i32,littleEndian?: bool)``` | ```void``` |```view.setInt32(0,8)```|向二进制字符串放入一个32bit有符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setInt64``` | 方法 | 2 | ```(byteOffset: i32, value: i64,littleEndian?: bool)``` | ```void``` |```view.setInt64(0,8)```|向二进制字符串放入一64bit有符号整数|
| ```setUint8``` | 方法 | 2 | ```(byteOffset: i32, value: u8)``` | ```void``` |```view.setUint8(0,8)```|向二进制字符串放入一个8bit无符号整数|
| ```setUint16``` | 方法 | 2 | ```(byteOffset: i32, value: u16,littleEndian?: bool)``` | ```void``` |```view.setUint16(0,8)```|向二进制字符串放入一个16bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setUint32``` | 方法 | 2 | ```(byteOffset: i32, value: u32,littleEndian?: bool)``` | ```void``` |```view.setUint32(0,8)```|向二进制字符串放入一个32bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|
| ```setUint64``` | 方法 | 2 | ```(byteOffset: i32, value: u64,littleEndian?: bool)``` | ```void``` |```view.setUint64(0,8)```|向二进制字符串放入一个64bit无符号整数，默认使用大端编码，也可以指定```littelEndian```为```true```使用小端编码|

### Map<K,V>

1. Map<K,V> 表示通用键到通用值的映射。因为 TDS 智能合约不支持 ```null``` 类型，所以查询不存在的键的会导致错误。

```typescript
const map = new Map<i32,string>();

const str = map.get(1); // 这里会报错，因为键 1 没有对应的值

const str1 = map.has(1) ? map.get(1) : ""; // 通过检查值是否存在规避异常
```

2. Map<K,V> 的成员如下：


| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```new``` | 构造器 | 0   | -     |      ```Map<K, V>```     |  ```new Map<u32, string>();```    |  构造器    |
|  ```size```  | 字段       |  -        |    -    |  ```i32```    |  -   |   Map 中键值对的数量   |
|  ```clear```  |   方法    |    0      |    -      |     ```void```     | ```map.clear();``` | 清空一个 map |
| ```delete``` | 方法 | 1 | ```(key: K)``` | ```bool``` | ```map.delete(1);``` |  从 map 中删除一个键值对，如果要删除的 key 确实存在，返回 ```true```|
| ```get``` | 方法| 1 | ```(key: K)``` | ```V``` | ```map.get(1);``` | 从 map 中读取 ```key``` 对应的值，如果不存在则抛出异常 |
| ```keys``` | 方法| 0 | - | ```Array<K>``` | ```map.keys()``` | map包含的所有键 |
| ```values``` | 方法| 0 | -                                                            | ```Array<V>``` | ```map.values()``` | map包含的所有值 |


### Math 

1. Math 的成员如下：

   下表中类型参数```T``` 表示```f32```或者```f64```


| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| ```E``` | 静态字段 | -  | -     |     ```T```     |  ```Math.E```  |  自然底数 e  |
|  ```PI```  | 静态字段   |  -        |    -    | ```T``` |  ```Math.PI```  |   圆周率   |
|  ```abs```  |   静态方法  |    1     | ```(x: T)``` |     ```T```     | ```Math.abs(-1)``` | 求绝对值 |
| ```acos``` | 静态方法 | 1 | ```(x: T)``` | ```T``` | ```Math.acos(1)``` | 求反余弦 |
| ```cos``` | 静态方法 | 1 | ```(x: T)``` | ```T```                                | ```Math.cos(1)``` | 求余弦 |
| ```asin``` | 静态方法 | 1 | ```(x: T)``` | ```T```  | ```Math.asin(1)``` | 求反正弦 |
| ```sin``` | 静态方法 | 1 | ```(x: T)```                                       | ```T```  | ```Math.sin(1)``` | 求正弦 |
| ```atan``` | 静态方法 | 1 | ```(x: T)``` | ```T```  | ```Math.atan(1)``` | 求反正切 |
| ```tan``` | 静态方法 | 1 | ```(x: T)```                                       | ```T```  | ```Math.tan(1)``` | 求正切 |
| ```max``` | 静态方法 | 2 | ```(value1: T, value2: T)``` | ```T```  | ```Math.max(2.0, 1.0)``` | 两个浮点数的较大的值 |
| ```min``` | 静态方法 | 2 | ```(value1: T, value2: T)``` | ```T```  | ```Math.min(2.0, 1.0)``` | 两个浮点数的较小的值 |
| ```pow``` | 静态方法 | 2 | ```(value1: T, value2: T)``` | ```T``` | ```Math.pow(2.0, 3.0)``` | 指数运算 |
| ```log``` | 静态方法 | 1 | ```(x: T)``` | ```T``` | ```Math.log(2)``` | 求自然对数 |
| ```ceil``` | 静态方法 | 1 | ```(x: T)``` | ```T``` | ```Math.ceil(2.1)``` | 向上取整 |
| ```floor``` | 静态方法 | 1 | ```(x: T)``` | ```T``` | ```Math.floor(2.1)``` | 向下取整 |
| ```round``` | 静态方法 | 1 | ```(x: T)``` | ```T``` | ```Math.round(2.1)``` | 4舍5入取整 |

## 智能合约开发

###  下载智能合约编写模版

``` sh
git clone https://github.com/TrustedDataFramework/assembly-script-template
cd assembly-script-template
npm install
```

在 ```package.json``` 中有两个重要的依赖项：

``` json
{
  "@salaku/js-sdk": "^0.2.10",
  "@salaku/sm-crypto": "^0.1.8"
}
```

其中 [```@salaku/sm-crypto```](https://github.com/TrustedDataFramework/sm-crypto) 包含了国密 sm2、sm3 和 sm4 的 javascript 实现，我们需要它来作哈希值计算和事务的签名等，[` ` `@salaku/js-sdk` ``](https://github.com/TrustedDataFramework/js-sdk) 封装了事务构造和rpc调用的方法，可以简化智能合约的开发。

### 编译和部署合约

1. 新建 local 目录

``` sh
mkdir local # local 文件夹在 git 中被忽略了
```

2. 编写合约源代码

然后新建一个 hello-world.ts 文件，复制以下内容到 hello-world.ts 中

``` sh
touch local/hello-world.ts
```

``` typescript
import {log} from "../lib";

// every contract should contains a function named 'init'
// which will be called when contract deployed
export function init(): void{
  log('hello world');
}

export function invoke(): void{
    log("hello world");
}
```

3. 编写配置文件 

再新建 config.json 文件，复制以下内容到 config.json

``` sh
touch local/config.json
```

``` json
{
  "version": "1634693120",
  "host": "192.168.1.171",
  "port": "7010",
  "source": "local/hello-world.ts",
  "private-key": "**",
  "asc-path": "node_modules/.bin/asc",
  "gas-price": 0
}
```

  + 各个字段说明如下：

    - version 表示事务的版本
    - host 表示合约部署的节点的主机
    - port 表示节点的 rpc 端口号
    - source 表示合约的源代码文件
    - private-key 需要填写私钥，用于对事务作签名
    - asc-path 是编译器的路径，对于 linux 和 mac 一般是 ```node_modules/.bin/asc```，对于 windows 一般是 ```node_modules/.bin/asc.cmd```
    - gas-price 表示手续费的单价，对于私链或者联盟链一般填0即可

4. 读取配置文件

利用 nodejs 提供的 require 函数可以轻松地读取 json 文件，因为我们新建的文件 ```config.json``` 位于 local 目录下，所以我们需要通过环境变量的方式将配置文件的路径传递过去

```js
const path = require('path')

function getConfigPath() {
  // 如果环境变量CONFIG=没有值，则选择当前目录下的 config.json 文件
    if (!process.env['CONFIG'])
        return path.join(process.cwd(), 'config.json')
      
    const c = process.env['CONFIG']
    // 判断环境变量的CONFIG=的值是否是绝对路径，如果是绝对路径直接 return 
    if (path.isAbsolute(c))
        return c
    // 如果是相对路径就把工作目录和这个相对路径拼接得到绝对路径    
    return path.join(process.cwd(), c)
}

// 读取配置
// 现在我们可以通过环境变量 CONFIG 传递配置文件的路径
const conf = require(getConfigPath());
```

5. 编译 

可以使用 ```@salaku/js-sdk``` 提供的方法编译合约：

``` js
const tool = require('@salaku/js-sdk')
// getConfigPath 的代码参考 5.配置
const conf = require(getConfigPath());

async function main() {
    // 编译合约源代码得到wasm二进制字节码，注意 wasm 的类型是 buffer
    const wasm = await tool.compileContract(conf['asc-path'], conf.source)
}
```

6. 构造事务

``` js
// 事务构造工具
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
```

有了事务构造工具和wasm字节码，就可以构造合约部署的事务了

``` js
const tx = builder.buildDeploy(wasm, 0)
```

7. 创建 rpc 工具，签发事务

``` js
const rpc = new tool.RPC(conf.host, conf.port)
```

有了 rpc 工具后，可以获取实时的 nonce，新的事务需要填充 nonce 后再作签名

``` js
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥
tx.nonce = (await rpc.getNonce(pk)) + 1 // 用公钥获取 nonce
builder.sign(tx) // 对事务作签名
const resp = await rpc.sendTransaction(tx) // 发送事务得到节点的返回值
```

8. 完整代码(deploy.js):

``` js
/**
 * 智能合约部署示例
 */

function getConfigPath() {
    if (!process.env['CONFIG'])
        return path.join(process.cwd(), 'config.json')
    const c = process.env['CONFIG']
    if (path.isAbsolute(c))
        return c
    return path.join(process.cwd(), c)
}

const tool = require('@salaku/js-sdk')
const path = require('path')

// 读取配置
const conf = require(getConfigPath());
const sk = conf['private-key']
const pk = tool.privateKey2PublicKey(sk)

// 事务构造工具
const builder = new tool.TransactionBuilder(conf.version, sk, conf['gas-price'] || 0)
// rpc 工具
const rpc = new tool.RPC(conf.host, conf.port)

async function main() {
    // 编译合约得到二进制内容
    const o = await tool.compileContract(conf['asc-path'], conf.source)
    // 构造合约部署的事务
    const tx = builder.buildDeploy(o, 0)

    if (!tx.nonce) {
        tx.nonce = (await rpc.getNonce(pk)) + 1
    }

    tool.sign(tx, sk)
    console.log( `contract address = ${tool.getContractAddress(pk, tx.nonce)}` )
    const resp = await tool.sendTransaction(conf.host, conf.port, tx)
    console.log(resp)
}

main().catch(console.error)
```

调用方式 

``` sh
CONFIG=local/config.json node deploy.js
```

### 合约代码结构

1. 函数声明

一份智能合约代码可以由一个或者多个源代码文件组成，只有合约中被声明为 export 的函数才可以被触发

``` typescript
import {log} from "../lib";
export function init(): void{ 
  log('hello world');
}

export function invoke(): void{ 
  execute();
}

function execute(): void{ // init 没有被 export
  log('hello world');
}
```

在这份合约中，invoke 函数可以通过构造事务或者通过rpc触发，而 execute 则不能被触发。

2. init 函数

每份合约都至少要包含一个名为 init 的函数，而且这个函数必须要被导出，这个 init 函数中的代码会在合约被部署时调用。

错误的合约(init 没有被 export)：

``` typescript
import {log} from "../lib";
function init(): void{ // init 没有被 export
  log('hello world');
}
```

错误的合约(缺少 init 函数)

``` typescript
import {log} from "../lib";
export function main(): void{ //  缺少 init 函数
  log('hello world');
}
```

### 状态存储 <span id="anchor1"></span>

1. 临时存储

和 solidity 不同，TDS 合约代码不通过声明全局变量的方式实现持久化存储，例如在以下代码中：

``` typescript
let c: u64;

export function init(): void{
  c = 0;
}

export function inc(): void{
  c++;
}
```

在这份合约中，c 被声明为全局变量，而且在外部可以通过构造事务触发 inc 函数实现 c 的自增，看似只要每次调用 inc 函数 c 都会加一。实际上在这里 c 存储的位置是 wasm 引擎的内存，而 wasm 引擎的内存不会被持久化到区块链中去，c本质上是一个临时存储。因此 inc 函数无论触发了多少次，c 的数值依然都是 0。

2. 永久存储

TDS 智能合约提供了实现永久存储的方法，由内置对象 ```DB``` 实现，```DB``` 本质上是一个 key value 存储，存储的 key 和 value 都是二进制格式，Assemblyscript 使用 Uint8Array 表示二进制的数据。

3. DB 类基本操作

```typescript
import {DB, DBIterator} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

export function init(): void{
  // 保存一个字符串键值对 （增、改）
  DB.set(str2bin('key'), str2bin('value'));

  // 删除一个字符串键值对 （删）
  DB.remove(str2bin('key'));

  // 判断 key 是否存在 （查）
  const exists = DB.has(str2bin('key'));

  if(!exists){
    DB.set(str2bin('key'), str2bin('value'));
  } else{
      // 打印 value 的值 （查）
      // 因为 Assemblyscript 没有 null 类型，如果 exists 为 false 的情况下调用 DB.get(str2bin('key')) 会报异常，合约执行会终止
    log(bin2str(DB.get(str2bin('key')));
  }
}
```

4. 迭代器 DBIterator

使用迭代器可以遍历整个合约状态存储

```typescript
import {DB, DBIterator, log, Context} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

export function iterate(): void{
  DBIterator.reset();
  while(DBIterator.hasNext()){
    const entry = DBIterator.next();
    log('key = ' + bin2str(entry.key) + ' value = ' + bin2str(entry.value));
  }
}
```

### 触发 <span id="anchor0"></span>

触发合约中的方法有两种方式，一种是通过 rpc 触发，另一种是通过事务触发。

1. rpc 触发

通过 rpc 触发的限制在于，触发的方法对合约状态存储必须是只读的，而且无法获得区块链的上下文对象，例如当前的事务、父区块的哈希值，在以下合约中：

```typescript
import {DB, DBIterator, log, Result} from './lib'

// 在保存字符串键值对前，要先把字符串转成二进制数据
function str2bin(str: string): Uint8Array{
  return Uint8Array.wrap(String.UTF8.encode(str));
}

// 把从 DB 中读取的二进制数据转成字符串
function bin2str(bin: Uint8Array): string{
  return String.UTF8.encode(bin.buffer);
}

// 把 key 设置为 0
export function init(): void{
  DB.set(str2bin('key'), str2bin('0'));
}

// 把 key 自增
export function inc(): void{
  const val = DB.get(str2bin('key'));
  const v = parseInt(bin2str(val));
  v++;
  DB.set(str2bin('key'), str2bin(v.toString()));
}

// 打印 key
export function logKey(): void{
  const val = DB.get(str2bin('key'));
  log(bin2str(val));
}
```

在这份合约中，```inc``` 函数对合约状态作了修改，因为无法通过 rpc 触发 ```inc``` 函数，而 ```logKey``` 函数没有对合约状态作修改，属于只读函数，所以可以用 rpc 触发 ```logKey``` 函数。

我们可以通过 rpc 触发合约中的只读函数来获取合约状态，而内置对象 ```Result``` 是 rpc 的返回值与合约函数传递接口，我们需要通过 ```Result``` 提供的 ```write``` api 来获取合约中的数据。例如我们在上文的合约的基础上增加一个函数 ```getKey```。

```typescript
export function getKey(): void{
  const val = DB.get(str2bin('key'));
  Result.write(val);
}
```

```Result.write``` 接受二进制格式的数据，并在外部应用中以十六进制编码的方式呈现，在外部展示合约中的值还需要转码，

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
async function main(){
  const data = await rpc.viewContract('***合约地址***', 'getKey')
  const val = Buffer.from(data, 'hex').toString('utf8')
  console.log(`val = ${val}`)
}

main()
```

2. rpc 触发时传参

rpc 触发合约中的函数时，可以用内置对象 ```Context``` 获取参数，例如我把 ```getKey``` 稍作修改，改成 ```getKeyAddN``` 函数

```typescript
export function getKeyAddN(): void{
  // 读取额外参数
  const p = Context.args().parameters;
  // 转码为 int 类型
  const j = parseInt(bin2str(p));
  // 读取 db 中的数值
  const val = parseInt(bin2str(DB.get(str2bin('key'))));
  // 作加法后返回
  Result.write(str2bin((j + val).toString()));
}
```

rpc 触发代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
async function main(){
  const data = await rpc.viewContract('***合约地址***', 'getKeyAddN', Buffer.from('128', 'ascii'))
  const val = Buffer.from(data, 'hex').toString('utf8')
  console.log(`val = ${val}`)
}

main()
```

3. 事务触发

通过事务触发可以对合约状态作写入、删改等操作，也可以在触发的函数中获取到区块链的上下文对象。


例如要通过事务触发以上合约中的 ```inc``` 函数可以执行 nodejs 代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥

async function main(){
  const tx = builder.buildContractCall('**合约地址**', tool.buildPayload('inc'), 0)
  tx.nonce = (await rpc.getNonce(pk)) + 1

  builder.sign(tx)
  rpc.sendTransaction(tx)
}

main()
```

4. 事务触发时带参

事务触发合约中的函数时，也可以用内置对象 ```Context``` 获取参数，例如我把 ```inc``` 稍作修改，改成 ```incN``` 函数

```typescript
export function incN(): void{
  // 读取额外参数
  const p = Context.args().parameters;
  // 转码为 int 类型
  const j = parseInt(bin2str(p));
  // 读取 db 中的数值
  const val = parseInt(bin2str(DB.get(str2bin('key'))));
  // 作加法后存储
  DB.set(str2bin('key'), str2bin((p + j).toString()));
}
```

例如要通过事务触发以上合约中的 ```incN``` 函数，把 ```key``` 对应的值加上 128 可以执行 nodejs 代码：

```js
const tool = require('@salaku/js-sdk')
const rpc = new tool.RPC(conf.host, conf.port)
const builder = new tool.TransactionBuilder(conf.version, conf['private-key'], conf['gas-price'] || 0)
const pk = tool.privateKey2PublicKey(conf['private-key']) // 把私钥转公钥

async function main(){
  const payload = tool.buildPayload('incN', Buffer.from('128', 'ascii'))
  const tx = builder.buildContractCall('**合约地址**', payload, 0)
  tx.nonce = (await rpc.getNonce(pk)) + 1
  builder.sign(tx)
  rpc.sendTransaction(tx)
}

main()
```

### 内置对象

我们利用了 Assemblyscript 丰富的扩展性，内建了一些对象和类，使得智能合约的编写更加简单，以下列举了内置对象的使用示例

1. log 函数

区块链节点在执行 log 函数时，会把 log 的参数打印到标准输出，可以用于调试智能合约

```typescript
import {log} from './lib'

export function init(): void{
  log('hello world');
}
```

2. Context 类

对于 rpc 触发的函数，可以通过 Context 拿到 rpc 调用的参数，对于事务触发的函数，可以通过 Context 拿到事务中payload 的参数，具体可以参考 [触发](#anchor0) 章节。

除此之外，事务触发的函数或者 init 函数可以通过 Context 拿到区块链上下文对象，示例如下：

```typescript
import {Context} from './lib'

export function init(): void{
  // 获得事务所在的区块的区块头
  // 包含了父区块的哈希，区块的创建时间和区块的高度
  const header = Context.header();

  // 获得当前的事务，包含了事务的所有字段
  const tx = Context.transaction();

  // 当前的合约的地址，部署时的nonce和合约的创建者的地址
  const contract = Context.contract();
}
```

3. DB 

DB 的使用可以参考[状态存储](#anchor1) 章节。

4. Decimal

Decimal 类用于实现精确的十进制有限小数的四则运算


```typescript
import {Decimal} from './lib'

export function init(): void{
  // 加法
  let d = Decimal.add('0.01', '0.02');

  // 减法
  d = Decimal.sub(d, '0.02');

  // 乘法
  d = Decimal.mul(d, 10);

  // 除法，限制小数点后10位，如果除不尽则抛出异常
  d = Decimal.div(d, 2, 10);

  // 比较
  let c = Decimal.compare('0.01', '0.02');
}
```

5. Event 类

Event 用于从合约内部向外界异步通知，区块链节点在启动时会创建一个 socketio 服务，默认的端口是 10004，外部程序可以在连接 socketio 服务后监听智能合约的事件。

例如以下合约部署后，在前端可以监听 emit 函数触发时发送的 ```event-name``` 事件

```typescript
import  from './lib'

export function init(): void{

}

export function emit(): void{
  const arr = new Uint8Array(1);
  arr[0] = 255;
  Event.emit('event-name', arr);
}
```

前端代码：

```html
<html>
  <body>
    <script type="text/javascript">
      function connectSocket() {
          let socket = window.socket
          if(socket != null){
              socket.disconnect()
          }
          window.socket = io.connect('http://localhost:10004')
          socket = window.socket
          socket.on('***合约地址***:event-name', console.log)
      }
      connectSocket()          
    </script>
  </body>
</html>
```

因为 [255] 的十六进制编码是 "ff" 当 emit 被触发时，前端将会打印出对象:

```json
{
  "data": "ff"
}
```

6. Hex 类

Hex 包含了十六进制编码和解码

```typescript
import {Hex} from './lib'

export function init(): void{
  // 十六进制编码转二进制数组
  const arr = Hex.decode('ff');

  // 二进制数组转十六进制编码
  log(Hex.encode(arr));
}
```

7. Hash 类

Hash 包含了常用的哈希值算法

```typescript
import {Hash} from './lib'

export function init(): void{
  // keccak256
  let digest = Hash.keccak256(Hex.decode('ff'));

  // sm3
  digest = Hash.sm3(Hex.decode('ff'));
}
```

### RLP 编码/解码


TDS 智能合约至提供了二进制 key-value 的存储的接口，但是在合约中的对象不一定是二进制型式的，所以需要在存储和读取时进行相应的类型转换。


对象编码的方式是将对象中的字段用递归的方式转成树形结构，以下是对一个寄件人进行 rlp 编码和反编码的参考

```typescript
// 寄件人
class Sender {
    constructor(
        // 地址
        public address: Uint8Array,
        // 邮寄物品
        public type: string,
        // 真实姓名
        public name: string,
        // 身份证号
        public id: string,
        // 电话号码
        public phone: string,
        // 简单说明
        public description: string,
        // 上链高度
        public height: u64,
        // 上链的事务哈希
        public hash: Uint8Array,
    ) {
    }

    // 从 rlp 解码
    static fromEncoded(data: Uint8Array): Sender {
        const li = RLPList.fromEncoded(data);
        const r = new RLPListReader(li);
        return new Sender(
            r.bytes(), r.string(), r.string(),
            r.string(), r.string(), r.string(),
            r.u64(), r.bytes()
        )
    }

    // rlp 编码
    getEncoded(): Uint8Array {
        const elements = [
            RLP.encodeBytes(this.address),
            RLP.encodeString(this.type),
            RLP.encodeString(this.name),
            RLP.encodeString(this.id),
            RLP.encodeString(this.phone),
            RLP.encodeString(this.description),
            RLP.encodeU64(this.height),
            RLP.encodeBytes(this.hash)
        ];
        return RLP.encodeElements(elements);
    }
}
```



























