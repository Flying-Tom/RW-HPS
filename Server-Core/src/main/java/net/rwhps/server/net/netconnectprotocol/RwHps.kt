/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol

import net.rwhps.server.core.ServiceLoader
import net.rwhps.server.core.ServiceLoader.ServiceType
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.core.TypeConnect
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.net.core.server.packet.AbstractNetPacket
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.exp.ImplementedException

/**
 * 核心协议实现
 * @property netType NetType                        : 使用的Net协议类型
 * @property typeConnect TypeConnect                : 连接解析器
 * @property abstractNetPacket AbstractNetPacket    : NetPacket
 * @author RW-HPS/Dr
 */
open class RwHps(private val netType: IRwHps.NetType) : IRwHps {
    override val typeConnect: TypeConnect =
        try {
            val protocolClass = ServiceLoader.getServiceClass(ServiceType.Protocol, netType.name)
            ServiceLoader.getService(ServiceType.ProtocolType, netType.name, Class::class.java).newInstance(protocolClass) as TypeConnect
        } catch (e: ImplementedException) {
            object: TypeConnect {
                override fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect {
                    TODO("Not yet implemented")
                }

                override fun typeConnect(packet: Packet) {
                    TODO("Not yet implemented")
                }
                override val abstractNetConnect: AbstractNetConnect get() = TODO("Not yet implemented")
                override val version: String get() = TODO("Not yet implemented")
            }
        } catch (e: Exception) {
            Log.fatal(e)
            throw ImplementedException("Not yet implemented")
        }


    override val abstractNetPacket: AbstractNetPacket =
        try {
            try {
                ServiceLoader.getService(ServiceType.ProtocolPacket, netType.name)
            } catch (e: ImplementedException) {
                ServiceLoader.getService(ServiceType.ProtocolPacket, IRwHps.NetType.ServerProtocol.name)
            }.newInstance() as AbstractNetPacket
        } catch (e: Exception) {
            Log.fatal(e)
            throw ImplementedException("Not yet implemented")
        }
}