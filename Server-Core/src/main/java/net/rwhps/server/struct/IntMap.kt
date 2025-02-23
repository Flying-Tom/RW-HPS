/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.struct

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

/**
 * @author RW-HPS/Dr
 */
class IntMap<V>: BaseMap<Int, V> {
    @JvmOverloads constructor(threadSafety: Boolean = false): this(16, threadSafety)

    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @JvmOverloads constructor(capacity: Int, threadSafety: Boolean = false): super(
        Int2ObjectOpenHashMap<V>(capacity).let {
            if (threadSafety) {
                Int2ObjectMaps.synchronize<V>(it, it)
            } else {
                it
            }
        } as java.util.Map<Int, V>, threadSafety
    )
}