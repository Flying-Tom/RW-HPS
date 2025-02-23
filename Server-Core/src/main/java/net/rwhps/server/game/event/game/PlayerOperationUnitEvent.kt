/*
 *
 *  * Copyright 2020-2023 RW-HPS Team and contributors.
 *  *
 *  * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *  *
 *  * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 *
 */

package net.rwhps.server.game.event.game

import net.rwhps.server.data.player.PlayerHess
import net.rwhps.server.game.GameUnitType
import net.rwhps.server.game.event.AbstractEvent

/**
 * 玩家操作单位事件
 *
 * @date 2023/7/5 16:15
 * @author RW-HPS/Dr
 */
class PlayerOperationUnitEvent(
    val player: PlayerHess, val gameActions: GameUnitType.GameActions,
    val gameUnits: GameUnitType.GameUnits, val x: Float, val y: Float) : AbstractEvent {
    // 操作是否有效
    @JvmField
    var resultStatus = true
}