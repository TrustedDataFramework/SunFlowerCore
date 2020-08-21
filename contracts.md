# TDS 智能合约编写指引 (AssemblyScript 篇)

[TOC]

## AssemblyScript 教程


### AssemblyScript 简介


AssemblyScript 是 TypeScript 的一个变种，和 TypeScript 不同，AssemblyScript 使用严格类型。


### AssemblyScript 基础类型

1. AssemblyScript 每个变量的类型是不可变的。AssembyScript 中的类型分为两种，一种是基本类型，另一种是引用类型。AssemblyScript 的所有基本类型列举如下：

| AssemblyScript 类型 | WebAssembly 类型 | 描述              |
|---------------------|------------------|-------------------|
| i32                 | i32              | 32 bit 有符号整数 |
| u32                 | u32              | 32 bit 无符号整数 |
| i64                 | i64              | 64 bit 有符号整数 |
| u64                 | u64              | 64 bit 无符号整数 |
| f32                 | f32              | 单精度浮点数      |
| f64                 | f64              | 双精度浮点数      |
| i8                  | i32              | 8 bit 有符号整数  |
| u8                  | i32              | 8 bit 无符号整数  |
| i16                 | i32              | 16 bit 有符号整数 |
| bool                | i32              | 布尔型            |


除了以上表中的基本类型以外的其他类型都是引用类型。


2. 类型转换

当 AssemblyScript 编译器检查到存在可能不兼容的隐式类型转换时，编译会以异常结果终止。如果需要进行可能不兼容的类型转换，请使用强制类型转换。

在AssemblyScript中，以上提到的每一个类型都有对应的强制转换函数。例如将一个 64 bit 无符号整数 类型的整数强制转换为 32 bit 无符号整数：

```typescript
const i: u64 = 123456789;
const j = u64(i);
```

3. 类型声明

AssemblyScript编译器必须在编译时知道每个表达式的类型。这意味着变量和参数在声明时必须同时声明其类型。如果没有声明类型，编译器将首先假定类型为i32，在数值过大时再考虑 i64，如果是浮点数就是用 f64。如果变量是其他函数的返回值，则变量的类型是函数返回值的类型。此外，所有函数的返回值类型都必须事先声明，以帮助编译器类型推断。

合法的函数：

```typescript
function sayHello(): void{
    log("hello world");
}
```

语法不正确的函数：


```typescript
function sayHello(): {
    log("hello world");
}
```

4. 空值

许多编程语言具有一个特殊的 null 类型表示空值，例如 javascript 和 java 的 null, go 语言和 python 的 nil。事实上 null 类型的引入给程序带来了许多不可预知性，空值检查的遗漏会给智能合约带来安全隐患，因此 TDS 智能合约的编写没有引入 null 类型。


5. 类型转换兼容性

在下表中，列出了所有基本类型的转换兼容性，打勾向表示从左右到右可以进行隐式的类型转换。





| ↱       | bool | i8/u8 | i16/u16 | i32/u32 | i64/u64 | f32 | f64 |
|---------|------|-------|---------|---------|---------|-----|-----|
| bool    | ✓    | ✓     | ✓       | ✓       | ✓       | ✓   | ✓   |
| i8/u8   |      | ✓     | ✓       | ✓       | ✓       | ✓   | ✓   |
| i16/u16 |      |       | ✓       | ✓       | ✓       | ✓   | ✓   |
| i32/u32 |      |       |         | ✓       | ✓       |     | ✓   |
| i64/u64 |      |       |         |         | ✓       |     |     |
| f32     |      |       |         |         |         | ✓   | ✓   |
| f64     |      |       |         |         |         |     | ✓   |


6. 数值比较

当使用比较运算符 ```!=``` 和 ```==``` 时，如果两个数值在类型转换时是兼容的，则不需要强制类型转换就可以进行比较。

操作符 ```>```，```<```，```>=```，```<=``` 对无符号整数和有符号整数有不同的比较方式，被比较的两个数值要么都是有符号整数，要么都是无符号整数，且具有转换兼容性。


7. 移位操作

移位操作符 ```<<```，```>>``` 的结果类型是操作符左端的类型，右端类型会被隐式转换成左端的类型。如果左端类型是有符号整数，执行算术移位，如果左端是无符号整数，则执行逻辑移位。

无符号右移操作符 ```>>>``` 类似，但始终执行逻辑移位。

### 模块化

一个 AssemblyScript 智能合约项目可能由多个文件组成，文件与文件之间可以存在互相引用的关系，互相使用对方导出的内容。。AssemblyScript 项目编译成 wasm 字节码时，需要指定一个入口文件，只有这个入口文件中被导出的函数才可以在将来被调用到。

1. 函数导出


```typescript
export function add(a: i32, b: i32): i32 {
  return a + b
}
```


2. 全局变量导出

```typescript
export const foo = 1
export var bar = 2
```


3. 类导出

```typescript
export class Bar {
    a: i32 = 1
    getA(): i32 { return this.a }
}
```

4. 导入

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

### 标准库


1. 全局变量

| 变量名   | 类型         | 描述                                   |
|----------|--------------|----------------------------------------|
| NaN      | f32 或者 f64 | not a number，表示不是一个有效的浮点数 |
| Infinity | f32 或者 f64 | 表示无穷大   -Infinity 表示无穷小      |


2. 全局函数

| 函数名     | 参数个数 | 参数列表               | 返回值类型 | 描述                                                         |
| ---------- | -------- | ---------------------- | ---------- | ------------------------------------------------------------ |
| isNaN      | 1        | f32 或 f64 | bool       | 判断一个浮点数是否无效                                       |
| isFinite   | 1        | f32 或 f64 | bool       | 判断一个浮点数满足：1. 不是无穷大 2. 不是无穷小 3. 有效      |
| parseInt   | 1 或者 2 | (string, radisx?: i32) | i64        | 从字符串解析成一个整数，radix等于10则使用 10 进制，默认 radix 是 10 |
| parseFloat | 1        | (string)               | f64        | 从字符串解析成一个浮点数，使用10进制                         |

3. 数组（Array）

AssemblyScript 中的 ```Array<T>``` 与 JavaScript 中的 Array 非常相似。区别在于除了基本类型以外的数组初始化后，数组中的元素必须显示初始化后才可以访问。例如：


- 使用基本类型初始化：

```typescript
const arr = new Array<u64>(10); // 使用基本类型 u64 创建数组
const zero = arr[0]; // zero 的值是 0，类型是 u64
```

- 使用引用类型初始化：

```typescript
const arr = new Array<string>(10); // 使用基本类型 u64 创建数组
const zero = arr[0]; // 因为 TDS 合约不允许 null 值，所以这里会报错，因为 arr[0] 没有被初始化

// 正确的做法是进行初始化
for(let i = 0; i < arr.length; i++){
    arr[i] = "";
}
```


- Array 类常用的成员：

| 名称 | 分类   | 参数个数 | 参数类型 | 返回值类型 | 示例 | 描述 |
|------|--------|----------|----------|------------|------|------|
| new  | 构造器 | 0或者1   | i32      |      ```Array<T>```     |  ```new Array<i32>(1);```    |  构造器    |
|  isArray    | 静态函数       |  1        |     任意    |  bool          |  ```Array.isArray(arr)```   |   判断一个变量是否是数组   |
|    length  |   字段     |    -      |    -      |     i32     | ```arr.length``` | 数组的长度 |
| concat | 方法 | 1 | ```Array<T>``` | ```Array<T>``` | ```arr0.concat(arr1)``` |  |
| every | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | bool | ```arr.every(fn)``` | 判断数组的每个元素是否都满足```fn``` |
| fill | 方法| 1、2或者3 | ```(value: T, start?: i32, end?: i32)``` | 返回自身 | ```arr.fill(0, 0, arr.length)``` | 对数组用```value```进行填充，```start```和```end```分别是填充的起始索引（包含）和结束索引（不包含） |
| filter | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | ```Array<T>``` | ```arr.filter(fn)``` | 过滤掉数组中不符合```fn```的元素 |
| findIndex | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | i32 | ```arr.findIndex(fn)``` | 获取到第一个满足```fn```的元素所在的索引或者```-1``` |
| forEach | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => void``` | ```void``` | ```arr.forEach(fn)``` | 用```fn```遍历数组 |
| includes | 方法| 1或2 | ```(value: T, fromIndex?: i32)``` | bool | ```arr.includes(1,0)``` | 判断数组是否包含```value``` |
| indexOf | 方法| 1或2 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | bool | - | 数组的每个元素是否都满足```fn``` |
| join | 方法| 1 | ```(sep: string)``` | string | ```arr.join(',')``` | 对数组中每个字符串用字符```sep``` 连接|
| lastIndexOf | 方法| 1或2 | ```(value: T, fromIndex?: i32)``` | i32 | ```arr.lastIndexOf('.')``` | 获取到最后等于```value```的元素所在的索引或者```-1``` |
| map | 方法| 1 | ```(fn: (value: T, index: i32, array: Array<T>) => U)``` | ```Array<U>``` | ```arr.map(fn)``` | 把数组```arr``` 的元素作为函数 ```fn``` 的参数映射出新数组 |
| pop | 方法| 0 | - | T | ```arr.pop()``` | 弹出数组的最后一个元素 |
| push | 方法| 1 | ```(value: T)``` | i32 | ```arr.push(1)``` | 向数组尾部增加一个元素，返回数组长度|
| reduce | 方法| 1或者2| ```(fn: (acc: U, cur: T, idx: i32, src: Array) => U, initialValue: U)``` | U | ```arr.reduce(fn, 0)``` | 从左端开始对数组进行累加操作，经常和 ```map``` 配合使用|
| reduceRight | 方法| 1或者2| ```(fn: (acc: U, cur: T, idx: i32, src: Array) => U, initialValue: U)``` | U | ```arr.reduceRight(fn, 0)``` | 从右端开始对数组进行累加操作|
| reverse | 方法| 0| - | 返回自身 | ```arr.reverse()`` | 把数组倒过来|
| shift | 方法| 0| - | T | ```arr.shift()`` | 弹出数组的第一个元素|
| slice | 方法| 1或2| ```(start?: i32, end?: i32)``` | ```Array<T>``` | ```arr.slice(0, arr.length)``` | 从数组的```start```（包含）截取到```end```（不包含）|
| some | 方法| 1 | ```fn: (value: T, index: i32, array: Array<T>) => bool``` | bool | ```arr.some(fn)``` | 判断数组中是否存在至少一个元素满足 ```fn```|
| sort | 方法 | 0 或 1 | ```fn?: (a: T, b: T) => i32``` | 返回自身 | ```arr.sort(fn)``` | 对数组进行排序，可以传入比较函数 ```fn``` |
| splice | 方法 | 1 或 2 | ```(start: i32, deleteCount?: i32)``` | ```Array<T>``` | ```arr.splice(1, 2)``` | 从数组中见截断一部分，start 表示开始截断的位置，deleteCount 表示截断掉多少个|
| unshift | 方法 | 1 | ```value: T``` | i32` | ```arr.unshift(1)``` | 在数组左端添加一个元素|



























