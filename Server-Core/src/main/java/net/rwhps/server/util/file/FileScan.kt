/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.file

import net.rwhps.server.struct.Seq
import net.rwhps.server.util.compression.CompressionDecoderUtils

/**
 * @author RW-HPS/Dr
 */
object FileScan {
    @JvmStatic
    fun scanPacketPathClass(packetPath: String): Seq<String> {
        val name = Seq<String>()
        CompressionDecoderUtils.zipAllReadStream(FileUtils.getMyCoreJarStream()).use {
            it.getZipAllBytes().eachAll { k, _ ->
                val packetPathRep = packetPath.replace(".", "/")+"/"
                if (k.startsWith(packetPathRep) && k.endsWith("class") && !k.contains("$")) {
                    if (!k.replace(packetPathRep,"").contains("/")) {
                        name.add(k.replace(".class","").replace("/","."))
                    }
                }
            }
        }
        return name
    }
}