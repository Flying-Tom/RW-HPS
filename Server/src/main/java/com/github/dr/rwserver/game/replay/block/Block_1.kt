/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package com.github.dr.rwserver.game.replay.block

import com.github.dr.rwserver.io.input.GameInputStream

class Block_1(intream: GameInputStream) {
    val unitAction: UnitAction
    /** 对应单位id ?  */
    // TODO
    val unit: String?

    /** 动作的目标位置 */
    var unitActionX = 1.0f
    var unitActionY = 1.0f

    /** 对应单位的id(单位把执行动作到..上) */
    var unitActionToId: Long = -1

    val d: Int

    val float_1: Float
    val float_2: Float

    val boolean_1: Boolean
    val boolean_2: Boolean
    val boolean_3: Boolean

    // TODO
    val string_1: String

    init {
        with (intream) {
            unitAction = readEnum(UnitAction::class.java) as UnitAction
            val unitId = readInt()
            unit = if (unitId == -2) readString() else null
            unitActionX = readFloat()
            unitActionY = readFloat()
            unitActionToId = readLong()
            d = readByte()
            float_1 = readFloat()
            float_2 = readFloat()
            boolean_1 = readBoolean()
            boolean_2 = readBoolean()
            boolean_3 = readBoolean()
            string_1 = isReadString()
        }
    }

    enum class UnitAction {
        move,
        attack,
        build,
        repair,
        loadInto,
        unloadAt,
        reclaim,
        attackMove,
        loadUp,
        patrol,
        guard,
        guardAt,
        touchTarget,
        follow,
        triggerAction,
        triggerActionWhenInRange,
        setPassiveTarget
    }
}