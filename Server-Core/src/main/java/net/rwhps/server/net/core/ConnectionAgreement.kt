/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core

import io.netty.channel.ChannelHandlerContext
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.game.event.global.NetConnectCloseEvent
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.GroupNet
import net.rwhps.server.net.handler.rudp.PackagingSocket
import net.rwhps.server.util.IPCountry
import net.rwhps.server.util.IpUtils
import net.rwhps.server.util.log.Log
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.*

/**
 * @author RW-HPS/Dr
 * @date 2021年2月2日星期二 06:31:11
 */
class ConnectionAgreement {
    private val protocolType: ((packet: Packet) -> Unit)
    private val objectOutStream: Any
    private val udpDataOutputStream: DataOutputStream?

    val isClosed: ()->Boolean
    val useAgreement: String
    val ip: String
    val ipLong24: String
    val ipCountry: String
    val ipCountryAll: String
    internal val localPort: Int
    val id: String = UUID.randomUUID().toString()

    /**
     * TCP Send
     * @param channelHandlerContext Netty-ChannelHandlerContext
     */
    internal constructor(channelHandlerContext: ChannelHandlerContext) {
        val channel = channelHandlerContext.channel()

        protocolType = { packet: Packet ->
            if (channel.isActive) {
                // 扔进Netty的线程处理, 不让netty包装成 Task, 避免有的是Task有的直接写入而导致的包顺序错误
                channel.eventLoop().execute {
                    channelHandlerContext.writeAndFlush(packet)
                }
            }
        }
        objectOutStream = channelHandlerContext
        udpDataOutputStream = null
        useAgreement = "TCP"
        isClosed = { false }

        ip = convertIp(channel.remoteAddress().toString())
        ipLong24 = IpUtils.ipToLong24(ip,false)
        ipCountry = IPCountry.getIpCountry(ip)
        ipCountryAll = IPCountry.getIpCountryAll(ip)
        localPort = (channel.localAddress() as InetSocketAddress).port
    }

    /**
     * UDP Send
     * @param socket Socket
     * @throws IOException Error
     */
    internal constructor(socket: PackagingSocket) {
        val socketStream = DataOutputStream(socket.outputStream)
        protocolType = { packet: Packet ->
            socketStream.writeInt(packet.bytes.size)
            socketStream.writeInt(packet.type.typeInt)
            socketStream.write(packet.bytes)
            socketStream.flush()
        }
        objectOutStream = socket
        udpDataOutputStream = socketStream
        useAgreement = "UDP"
        isClosed = { socket.isClosed }

        ip = convertIp(socket.remoteSocketAddressString)
        ipLong24 = IpUtils.ipToLong24(ip,false)
        ipCountry = IPCountry.getIpCountry(ip)
        ipCountryAll = IPCountry.getIpCountryAll(ip)
        localPort = socket.localPort
    }

    constructor() {
        protocolType = {}
        objectOutStream = ""
        udpDataOutputStream = null
        useAgreement = "Test"
        isClosed = { false }

        ip = ""
        ipLong24 = ""
        ipCountry = ""
        ipCountryAll = ""
        localPort = 0
    }

    fun add(groupNet: GroupNet) {
        if (objectOutStream is ChannelHandlerContext) {
            groupNet.add(objectOutStream.channel())
        } else if (objectOutStream is PackagingSocket) {
            groupNet.add(this)
        }
    }

    fun remove(groupNet: GroupNet?) {
        if (groupNet != null) {
            if (objectOutStream is ChannelHandlerContext) {
                groupNet.remove(objectOutStream.channel())
            } else if (objectOutStream is PackagingSocket) {
                groupNet.remove(this)
            }
        }
    }

    /**
     * 接管Send逻辑
     * 整合不同协议的发送逻辑
     * @param packet MsgPacket
     * @throws IOException Error
     */
    @Throws(IOException::class)
    @Synchronized
    fun send(packet: Packet) {
        // 保持包顺序
        protocolType(packet)
    }

    /**
     * 关闭连接
     *
     * @param groupNet     Multicast pools
     * @throws IOException Potential errors
     */
    @Throws(IOException::class)
    fun close(groupNet: GroupNet?) {
        PluginManage.runGlobalEventManage(NetConnectCloseEvent(this))

        remove(groupNet)

        if (objectOutStream is ChannelHandlerContext) {
            objectOutStream.channel().close()
            objectOutStream.close()
        } else if (objectOutStream is PackagingSocket) {
            try {
                udpDataOutputStream!!.close()
                objectOutStream.close()
            } catch (e : SocketException) {
                Log.debug("[RUDP Close] Passive")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other == null || javaClass != other.javaClass) {
            false
        } else id == (other as ConnectionAgreement).id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    private fun convertIp(ipString: String): String {
        return ipString.substring(1, ipString.indexOf(':'))
    }
}