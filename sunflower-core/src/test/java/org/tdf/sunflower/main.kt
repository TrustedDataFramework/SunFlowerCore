package org.tdf.sunflower

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    println("start")
    runBlocking {
        for (i in 0 until 100) {
            GlobalScope.launch { printCurrentThread() }
        }
    }
    println("end")
}

private suspend fun printCurrentThread() {
    delay(1000)
    print(".")
}




