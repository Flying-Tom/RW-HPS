/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin

import net.rwhps.server.data.global.Data
import net.rwhps.server.util.file.FileUtils
import java.util.*

/**
 * @author RW-HPS/Dr
 */
class PluginLoadData(
    @JvmField val name: String,
    @JvmField val author: String,
    @JvmField val description: String,
    @JvmField val version: String,
    @JvmField val main: Plugin,
    private val mkdir: Boolean = true,
    private val skip: Boolean = false
) {
    init {
        if (mkdir) {
            main.pluginDataFileUtils = FileUtils.getFolder(Data.Plugin_Plugins_Path,true).toFolder(this.name)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other == null || javaClass != other.javaClass) {
            false
        } else name == (other as PluginLoadData).name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}