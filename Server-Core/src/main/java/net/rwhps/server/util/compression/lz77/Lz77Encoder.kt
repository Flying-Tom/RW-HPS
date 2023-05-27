/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.lz77

import net.rwhps.server.util.compression.core.AbstractEncoder
import net.rwhps.server.util.io.IoRead
import net.rwhps.server.util.log.exp.CompressionException
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.InputStream

/**
 * @date  2023/5/27 12:33
 * @author  RW-HPS/Dr
 */
class Lz77Encoder : AbstractEncoder() {
    private val outputStream = SeekableInMemoryByteChannel()
    private val lz77OutCache = SevenZOutputFile(outputStream)

    @Throws(CompressionException.UnsupportedRatingsException::class)
    override fun setCompressibility(level: Int) {
        throw CompressionException.UnsupportedRatingsException("7z Not supported")
    }

    override fun addCompressBytes(name: String, inStream: InputStream?, bytes: ByteArray?) {
        val oze = SevenZArchiveEntry().apply {
            this.name = name
        }
        lz77OutCache.putArchiveEntry(oze)
        if (inStream == null) {
            lz77OutCache.write(bytes!!)
        } else {
            lz77OutCache.write(IoRead.readInputStreamBytes(inStream))
        }
        lz77OutCache.closeArchiveEntry()
    }

    override fun flash(): ByteArray {
        lz77OutCache.close()
        return outputStream.array()
    }
}