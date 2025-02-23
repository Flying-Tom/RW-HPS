/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import net.rwhps.server.data.global.Cache
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.PlayerHess
import net.rwhps.server.game.GameMaps
import net.rwhps.server.game.GameUnitType
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.GameCommandPacket
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.server.packet.AbstractNetPacket
import net.rwhps.server.net.netconnectprotocol.internal.server.*
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.IsUtils
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.annotations.MainProtocolImplementation
import java.io.IOException

/**
 * Provides support for most common packages for the server
 *
 * @author RW-HPS/Dr
 */
@MainProtocolImplementation
open class GameVersionPacket : AbstractNetPacket {
    @Throws(IOException::class)
    override fun getSystemMessagePacket(msg: String): Packet =
        chatMessagePacketInternal(msg, "SERVER", 5)

    @Throws(IOException::class)
    override fun getChatMessagePacket(msg: String, sendBy: String, team: Int): Packet =
        chatMessagePacketInternal(msg, sendBy, team)

    @Throws(IOException::class)
    override fun getPingPacket(player: PlayerHess): Packet {
        player.timeTemp = System.currentTimeMillis()
        val o = GameOutputStream()
        o.writeLong(0L)
        o.writeByte(0)
        return o.createPacket(PacketType.HEART_BEAT)
    }

    @Throws(IOException::class)
    override fun getTickPacket(tick: Int): Packet =
        gameTickPacketInternal(tick)

    @Throws(IOException::class)
    override fun getGameTickCommandPacket(tick: Int, cmd: GameCommandPacket): Packet =
        gameTickCommandPacketInternal(tick,cmd)

    @Throws(IOException::class)
    override fun getGameTickCommandsPacket(tick: Int, cmd: Seq<GameCommandPacket>): Packet =
        gameTickCommandsPacketInternal(tick,cmd)

    @Throws(IOException::class)
    override fun getPacketMapName(bytes: ByteArray): String {
        GameInputStream(bytes).use { stream ->
            stream.readString()
            stream.readInt()
            stream.readInt()
            return stream.readString()
        }
    }

    override fun getDeceiveGameSave(): Packet {
        val cPacket: Packet? = Cache.packetCache["getDeceiveGameSave"]
        if (IsUtils.notIsBlank(cPacket)) {
            return cPacket!!
        }

        val o = GameOutputStream()
        o.writeByte(0)
        o.writeInt(0)
        o.writeInt(0)
        o.writeFloat(0)
        o.writeFloat(0)
        o.writeBoolean(true)
        o.writeBoolean(false)
        val gzipEncoder = CompressOutputStream.getGzipOutputStream("gameSave", false)
        gzipEncoder.writeString("This is RW-HPS [Deceive Get GameSave]!")
        o.flushEncodeData(gzipEncoder)

        val cachePacket = o.createPacket(PacketType.SYNC)
        Cache.packetCache.put("getDeceiveGameSave",cachePacket)

        return cachePacket
    }

    override fun gameSummonPacket(index: Int, unit: String, x: Float, y: Float, size: Int): GameCommandPacket {
        val outStream = GameOutputStream()
        outStream.writeByte(index)
        outStream.writeBoolean(true)
        // 建造
        outStream.writeInt(GameUnitType.GameActions.BUILD.ordinal)


        var unitID = -2
        if (IsUtils.notIsNumeric(unit)) {
            GameUnitType.GameUnits.values().forEach {
                if (it.name.equals(unit,ignoreCase = true)) {
                    unitID = it.ordinal
                    return@forEach
                }
            }
        } else {
            unitID = unit.toInt()
        }

        outStream.writeInt(unitID)

        if (unitID == -2) {
            outStream.writeString(unit)
        }


        // X
        outStream.writeFloat(x)
        // Y
        outStream.writeFloat(y)
        // Tager
        outStream.writeLong(-1L)
        // 如果生成圆 那么这就是半径
        outStream.writeByte(size)
        outStream.writeFloat(1)
        outStream.writeFloat(1)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        //
        outStream.writeInt(-1)
        outStream.writeInt(-1)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        outStream.writeInt(0)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)


        outStream.writeLong(-1)
        outStream.writeString("-1")
        outStream.writeBoolean(false)
        outStream.writeShort(0)
        // System action
        outStream.writeBoolean(true)
        outStream.writeByte(0)
        outStream.writeFloat(0f)
        outStream.writeFloat(0f)
        //action type
        outStream.writeInt(5)
        outStream.writeInt(0)
        outStream.writeBoolean(false)
        return GameCommandPacket(index, outStream.getPacketBytes())
    }

    @Throws(IOException::class)
    override fun getExitPacket(): Packet {
        val cPacket: Packet? = Cache.packetCache["getExitPacket"]
        if (IsUtils.notIsBlank(cPacket)) {
            return cPacket!!
        }

        val cachePacket = playerExitPacketInternal()
        Cache.packetCache.put("getExitPacket",cachePacket)

        return cachePacket
    }
}