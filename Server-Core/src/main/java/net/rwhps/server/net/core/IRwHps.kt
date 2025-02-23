/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core

import net.rwhps.server.net.core.server.packet.AbstractNetPacket
import net.rwhps.server.util.annotations.NeedHelp

/**
 * RW-HPS Protocol 接口. 是 RW-HPS 协议实现的接口.
 *
 * ## 获取实例
 * 通常在服务器ServerLoad事件后 就可以使用 [ServiceLoader.getIRwHpsObject] 方法获取实例
 *
 * ## 使用 [IRwHps] 的接口
 * [IRwHps] 中的接口通常是稳定
 *
 * ## 稳定性
 * ### 使用稳定
 * 所有接口默认是可以稳定使用的
 *
 * ### 继承不稳定
 * **[IRwHps] 可能会增加新的抽象属性或函数. 因此不适合被继承或实现.**
 *
 * ## 接口
 * `IRwHps` 提供两个接口, 分别是
 *
 * ### typeConnect
 * 通过本接口 您可以获取到Packet解析器
 *
 * ### abstractNetPacket
 * 通过本接口 您可以获取到 `RW-HPS` 的 部分包协议实现
 * 核心部分实现由 [TypeConnect] 内的 `GameVersion*` 实现
 *
 * ## 注册
 * 在 `Initialization` 已经注册了默认实现
 * ```kotlin
 * ServiceLoader.addService(
 *     ServiceType.ProtocolType,
 *     IRwHps.NetType.ServerProtocol.name,
 *     TypeRwHps::class.java
 * )
 * ```
 * `RW-HPS` 强制性要求格式为
 * ```kotlin
 * ServiceLoader.addService(
 *     服务的类型(ServiceLoader.ServiceType),
 *     服务运行在何种协议.name (IRwHps.NetType),
 *     Class
 * )
 * ```
 *
 * ## 实例化 `IRwHps`
 * `IRwHps` 主要在 [net.rwhps.server.data.global.NetStaticData] 中被实例化
 *
 * 默认通过 [net.rwhps.server.core.ServiceLoader] 使用参数 `(ServiceType.IRwHps, ServerNetType.name, IRwHps.NetType::class.java)` 来完成
 *
 * 如果找不到对应实现, 则默认使用 `ServerNetType.name` 为 `IRwHps`, 因此, 你可以在覆盖协议时自定义协议接口的实现
 *
 * ## 获取实例
 * 当 `IRwHps` 被实例化时 它应该将 `typeConnect` `abstractNetPacket` 通过反射进行实例化, 如果失败则默认返回空实现
 *
 * ```kotlin
 * // RW-HPS
 * override val typeConnect: TypeConnect =
 *         try {
 *             val protocolClass = ServiceLoader.getServiceClass(ServiceType.Protocol,netType.name)
 *             ServiceLoader.getService(ServiceType.ProtocolType,netType.name,Class::class.java).newInstance(protocolClass) as TypeConnect
 *         } catch (e: Exception) {
 *             Log.fatal(e)
 *             NullTypeConnect()
 *         }
 *
 * override val abstractNetPacket: AbstractNetPacket =
 *     try {
 *         ServiceLoader.getService(ServiceType.ProtocolPacket,"ALLProtocol").newInstance() as AbstractNetPacket
 *     } catch (e: Exception) {
 *         Log.fatal(e)
 *         NullNetPacket()
 *     }
 * ```
 *
 * @author RW-HPS/Dr
 */
interface IRwHps {
    /**
     * 服务器的协议
     * 主要提供 包的解析 功能
     */
    val typeConnect: TypeConnect
    /**
     * 服务器Packet协议
     * 作为游戏功能的主要实现
     */
    val abstractNetPacket: AbstractNetPacket

    //val gameEngine:

    enum class NetType {
        /** (默认) Server协议 原汁原味服务器实现 */
        ServerProtocol,
        /** Server旧协议 提供过去版本的服务器实现 */
        ServerProtocolOld,
        /** Server协议 Server服务器实现 加入测试功能 */
        ServerTestProtocol,
        /** Relay协议 普通RELAY实现 */
        RelayProtocol,
        /** Relay协议 多播RELAY实现 */
        RelayMulticastProtocol,
        /** 专用后端 */
        DedicatedToTheBackend,
        /** 无实现 找不到对应实现 */
        NullProtocol;

        companion object {
            fun from(type: String?): NetType = values().find { it.name == type } ?: NullProtocol
        }
    }

    /**
     * 暂时没法分离 GameEngine
     */
    @NeedHelp
    enum class GameHeadlessType {
        GameEngine,
        GameData,
        GameNet;
    }
}