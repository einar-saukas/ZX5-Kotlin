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

class Decompressor {
    private var lastOffset1 = INITIAL_OFFSET
    private var lastOffset2 = 0
    private var lastOffset3 = 0
    private var inputData = ByteArray(0)
    private var output = MutableList<Byte>(0) { 0 }
    private var inputIndex = 0
    private var bitMask = 0
    private var bitValue = 0
    private var backwards = false
    private var inverted = false
    private var backtrack = false
    private var bitAhead = 0

    private fun restoreBit(bit: Int) {
        bitAhead = bit
        backtrack = true
    }

    private fun readByte() = inputData[inputIndex++]

    private fun readBit(): Int {
        if (backtrack) {
            backtrack = false
            return bitAhead
        }
        bitMask = bitMask shr 1
        if (bitMask == 0) {
            bitMask = 128
            bitValue = readByte().toInt()
        }
        return if (bitValue and bitMask != 0) 1 else 0
    }

    private fun readInterlacedEliasGamma(msb: Boolean): Int {
        var value = 1
        while (readBit() == if (backwards) 1 else 0) {
            value = (value shl 1) + readBit() xor if (msb && inverted) 1 else 0
        }
        return value
    }

    private fun writeByte(value: Byte) = output.add(value)

    private fun copyBytes(length: Int) {
        for (i in 1..length) {
            writeByte(output[output.size - lastOffset1])
        }
    }

    private fun createOffset(offset: Int) {
        lastOffset3 = lastOffset2
        lastOffset2 = lastOffset1
        lastOffset1 = offset
    }

    private fun promoteOffset2() {
        val offset = lastOffset2
        lastOffset2 = lastOffset1
        lastOffset1 = offset
    }

    private fun promoteOffset3() = createOffset(lastOffset3)

    fun decompress(input: ByteArray, backwardsMode: Boolean, invertMode: Boolean): ByteArray {
        lastOffset1 = INITIAL_OFFSET
        lastOffset2 = 0
        lastOffset3 = 0
        inputData = input
        output = MutableList(0) { 0 }
        inputIndex = 0
        bitMask = 0
        backwards = backwardsMode
        inverted = invertMode
        backtrack = false

        var state: State? = State.COPY_LITERALS
        while (state != null) {
            state = state.process(this)
        }
        return output.toByteArray()
    }

    enum class State {
        COPY_LITERALS {
            override fun process(d: Decompressor): State {
                val length = d.readInterlacedEliasGamma(false)
                for (i in 1..length) {
                    d.writeByte(d.readByte())
                }
                return if (d.readBit() == 0) COPY_FROM_LAST_OFFSET else COPY_FROM_OTHER_OFFSET
            }
        },
        COPY_FROM_LAST_OFFSET {
            override fun process(d: Decompressor): State {
                val length = d.readInterlacedEliasGamma(false)
                d.copyBytes(length)
                return if (d.readBit() == 0) COPY_LITERALS else COPY_FROM_OTHER_OFFSET
            }
        },
        COPY_FROM_OTHER_OFFSET {
            override fun process(d: Decompressor): State? {
                if (d.readBit() == 0) {
                    if (d.readBit() == 0) {
                        d.promoteOffset2()
                    } else {
                        d.promoteOffset3()
                    }
                    val length = d.readInterlacedEliasGamma(false)
                    d.copyBytes(length)
                } else {
                    val ahead = d.readBit()
                    val msb = d.readInterlacedEliasGamma(true)
                    if (msb == 256) {
                        return null
                    }
                    val lsb = d.readByte().toUByte().toInt()
                    val offset = if (d.backwards) msb * 256 + lsb - 255 else msb * 256 - lsb
                    d.createOffset(offset)
                    d.restoreBit(ahead)
                    val length = d.readInterlacedEliasGamma(false) + 1
                    d.copyBytes(length)
                }
                return if (d.readBit() == 0) COPY_LITERALS else COPY_FROM_OTHER_OFFSET
            }
        };

        abstract fun process(d: Decompressor): State?
    }
}
