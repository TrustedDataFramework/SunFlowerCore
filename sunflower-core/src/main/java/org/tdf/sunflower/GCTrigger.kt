package org.tdf.sunflower

import com.github.salpadding.rlpstream.Rlp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Component
import org.tdf.lotusvm.ModuleInstance
import org.tdf.lotusvm.runtime.UnsafeMemory
import org.tdf.lotusvm.runtime.UnsafeStackAllocator
import org.tdf.sunflower.vm.VMExecutor.*

//@Component
class GCTrigger {
    val tk = ticker(1000, 0, mode = TickerMode.FIXED_DELAY)

    init {
        GlobalScope.launch {
            for (x in tk) {
                val bytes = ByteArray(1024 * 1024 * 32)
                Rlp.encode(bytes)
                val moduleStream = Start.getCustomClassLoader()
                    .getResourceAsStream("main.wasm")


                val moduleBytes = IOUtils.toByteArray(moduleStream)
                val stack =
                    UnsafeStackAllocator(MAX_STACK_SIZE, MAX_FRAMES, MAX_LABELS)
                val mem = UnsafeMemory()
                try {
                    val ins = ModuleInstance
                        .builder()
                        .stackAllocator(stack)
                        .binary(moduleBytes)
                        .memory(mem)
                        .build()

                    ins.execute("bench")
                } finally {
                    stack.close()
                    mem.close()
                }
            }
        }
    }
}