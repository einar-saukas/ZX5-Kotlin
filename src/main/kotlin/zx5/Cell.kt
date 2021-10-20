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

private fun eliasGammaBits(value: Int): Int {
    var i = value
    var bits = 1
    while (i > 1) {
        bits += 2
        i = i shr 1
    }
    return bits
}

class Cell {
    var bits: Int
    var index: Int
    private val maps = HashMap<Offsets, Block>()

    constructor(bits: Int = Int.MAX_VALUE, index: Int = Int.MIN_VALUE) {
        this.bits = bits
        this.index = index
    }

    constructor(bits: Int, index: Int, offset: Int, length: Int) {
        this.bits = bits
        this.index = index
        maps[Offsets(offset, 0, 0)] = Block(bits, offset, length, null)
    }

    private fun prepare(bits: Int, index: Int): Boolean {
        if (this.index != index || this.bits > bits) {
            this.bits = bits
            this.index = index
            maps.clear()
            return true
        }
        return this.bits == bits
    }

    fun addLiteralBlock(index: Int, source: Cell) {
        val length = index - source.index
        val bits = source.bits + 1 + eliasGammaBits(length) + length * 8
        prepare(bits, index)
        for ((key, value) in source.maps) {
            maps[key] = Block(bits, 0, length, value)
        }
    }

    fun addLastOffsetBlock(index: Int, offset: Int, source: Cell) {
        val length = index - source.index
        val bits = source.bits + 1 + eliasGammaBits(length)
        if (prepare(bits, index)) {
            for ((key, value) in source.maps) {
                if (key.offset1 == offset && value.offset == 0) {
                    maps[key] = Block(bits, offset, length, value)
                }
            }
        }
    }

    fun addPreviousOffsetBlock(index: Int, offset: Int, source: Cell): Boolean {
        val length = index - source.index
        val bits = source.bits + 3 + eliasGammaBits(length)
        var found = false
        if (this.index != index || this.bits >= bits) {
            for ((key, value) in source.maps) {
                if (key.offset2 == offset || key.offset3 == offset) {
                    if (!found) {
                        prepare(bits, index)
                        found = true
                    }
                    maps[Offsets(offset, key.offset1, if (key.offset2 != offset) key.offset2 else key.offset3)] =
                        Block(bits, offset, length, value)
                }
            }
        }
        return found
    }

    fun addNewOffsetBlock(index: Int, offset: Int, source: Cell): Boolean {
        val length = index - source.index
        val bits = source.bits + 10 + eliasGammaBits((offset - 1) / 256 + 1) + eliasGammaBits(length - 1)
        if (prepare(bits, index)) {
            for ((key, value) in source.maps) {
                maps[Offsets(offset, key.offset1, key.offset2)] = Block(bits, offset, length, value)
            }
            return true
        }
        return false
    }

    fun mergeBlocks(source: Cell) {
        maps.putAll(source.maps)
    }

    fun anyBlock(): Block = maps.values.first()

    fun isValid(): Boolean = index != Int.MIN_VALUE
}
