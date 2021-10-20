/*
 * (c) Copyright 2021 by Einar Saukas. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * The name of its author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package zx5

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.*

const val INITIAL_OFFSET = 1
const val MAX_SCALE = 55

private fun offsetCeiling(index: Int, offsetLimit: Int): Int = min(max(index, INITIAL_OFFSET), offsetLimit)

class Optimizer {
    private var lastLiteral = emptyArray<Cell>()
    private var lastMatch = emptyArray<Cell>()
    private var optimal = emptyArray<Cell>()

    fun optimize(input: ByteArray, skip: Int, offsetLimit: Int, threads: Int, verbose: Boolean): Block {

        // allocate all main data structures at once
        val arraySize = offsetCeiling(input.size - 1, offsetLimit) + 1
        lastLiteral = Array(arraySize) { Cell() }
        lastMatch = Array(arraySize) { Cell() }
        optimal = Array(input.size) { Cell() }

        // start with fake block
        lastMatch[INITIAL_OFFSET] = Cell(-1, skip - 1, INITIAL_OFFSET, 0)

        var dots = 2
        if (verbose) {
            print("[")
        }

        // process remaining bytes
        var optimalBits: Int
        val pool = if (threads > 1) Executors.newFixedThreadPool(threads) else null
        for (index in skip until input.size) {
            val maxOffset = offsetCeiling(index, offsetLimit)
            if (pool == null) {
                optimalBits = processTask(1, maxOffset, index, skip, input)
            } else {
                val taskSize = maxOffset / threads + 1
                val tasks = LinkedList<Future<Int>>()
                for (firstOffset in 1..maxOffset step taskSize) {
                    val lastOffset = min(firstOffset + taskSize - 1, maxOffset)
                    tasks.add(pool.submit<Int> { processTask(firstOffset, lastOffset, index, skip, input) })
                }
                optimalBits = Int.MAX_VALUE
                for (task in tasks) {
                    val taskBits = task.get()
                    if (optimalBits > taskBits) {
                        optimalBits = taskBits
                    }
                }
            }

            // identify optimal choice so far
            optimal[index] = Cell(optimalBits, index)
            for (offset in 1..maxOffset) {
                if (lastMatch[offset].bits == optimalBits && lastMatch[offset].index == index) {
                    optimal[index].mergeBlocks(lastMatch[offset])
                } else if (lastLiteral[offset].bits == optimalBits && lastLiteral[offset].index == index) {
                    optimal[index].mergeBlocks(lastLiteral[offset])
                }
            }

            // indicate progress
            if (verbose && index * MAX_SCALE / input.size > dots) {
                print(".")
                dots++
            }
        }
        pool?.shutdown()

        if (verbose) {
            println("]")
        }

        return optimal[input.size - 1].anyBlock()
    }

    private fun processTask(firstOffset: Int, lastOffset: Int, index: Int, skip: Int, input: ByteArray): Int {
        var optimalBits = Int.MAX_VALUE
        for (offset in firstOffset..lastOffset) {
            if (index != skip && index >= offset && input[index] == input[index - offset]) {
                // copy from last offset
                if (lastLiteral[offset].isValid()) {
                    lastMatch[offset].addLastOffsetBlock(index, offset, lastLiteral[offset])
                    if (optimalBits > lastMatch[offset].bits) {
                        optimalBits = lastMatch[offset].bits
                    }
                }
                // copy from another offset
                var length = 1
                while (length <= index - offset + 1 && input[index - length + 1] == input[index - length - offset + 1]) {
                    if (lastMatch[offset].addPreviousOffsetBlock(index, offset, optimal[index - length]) ||
                        length > 1 && lastMatch[offset].addNewOffsetBlock(index, offset, optimal[index - length])
                    ) {
                        if (optimalBits > lastMatch[offset].bits) {
                            optimalBits = lastMatch[offset].bits
                        }
                    }
                    length++
                }
            } else {
                // copy literals
                if (lastMatch[offset].isValid()) {
                    lastLiteral[offset].addLiteralBlock(index, lastMatch[offset])
                    if (optimalBits > lastLiteral[offset].bits) {
                        optimalBits = lastLiteral[offset].bits
                    }
                }
            }
        }
        return optimalBits
    }
}
