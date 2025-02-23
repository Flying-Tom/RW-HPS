/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util

import com.sun.jna.Library
import com.sun.jna.Native
import net.rwhps.server.util.log.Log

/**
 * 操作控制台
 *
 * @date  2023/6/23 11:40
 * @author  RW-HPS/Dr
 */
object CLITools {
    /**
     * 在 Windows 上设置 Cmd 的标题
     *
     * @param title 需要设置的标题
     */
    fun setWindowsCmdTitle(title: String) {
        if (SystemUtils.isWindows) {
            CLibrary.INSTANCE.SetConsoleTitleA(title)
        } else {
            Log.debug("Not supported : setWindowsCmdTitle")
        }
    }

    private interface CLibrary : Library {
        fun SetConsoleTitleA(title: String?): Boolean

        companion object {
            val INSTANCE = Native.load("kernel32", CLibrary::class.java)
        }
    }
}