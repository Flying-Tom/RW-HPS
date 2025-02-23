/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

import net.rwhps.server.command.ex.Vote
import net.rwhps.server.core.Application
import net.rwhps.server.data.bean.BeanCoreConfig
import net.rwhps.server.data.bean.BeanRelayConfig
import net.rwhps.server.data.bean.BeanServerConfig
import net.rwhps.server.func.StrCons
import net.rwhps.server.game.Rules
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.net.http.WebData
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.util.I18NBundle
import net.rwhps.server.util.SystemUtils
import net.rwhps.server.util.file.LoadIni
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log
import org.jline.reader.LineReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 别问我为什么要把@JvmField和val并排
 * 问就是好看
 * @author RW-HPS/Dr
 */
object Data {
    /** 服务器主目录 */
    const val Plugin_Data_Path = "/data"
    /** 服务器save保存目录 */
    const val Plugin_Save_Path = "/data/save"
    /** 服务器缓存目录 */
    const val Plugin_Cache_Path = "/data/cache"
    /** 服务器依赖目录 */
    const val Plugin_Lib_Path = "/data/libs"
    /** 服务器日志目录 */
    const val Plugin_Log_Path = "/data/log"
    /** 服务器地图目录 */
    const val Plugin_Maps_Path = "/data/maps"
    /** 服务器插件目录 */
    const val Plugin_Plugins_Path = "/data/plugins"
    /** 服务器无头数据目录 */
    const val Plugin_GameCore_Data_Path = "/data/gameData"
    /** 服务器Mod目录 */
    const val Plugin_Mods_Path = "/data/mods"
    /** 服务器保存RePlay目录 */
    const val Plugin_RePlays_Path = "/data/replays"
    /** 服务器 UTF-8 缓存 */
    @JvmField val UTF_8: Charset = StandardCharsets.UTF_8
    /** 服务器默认编码缓存 */
    @JvmField val DefaultEncoding:  Charset = SystemUtils.defaultEncoding
    /*
	 * 插件默认变量
	 */
    /** 自定义包名  */
    const val SERVER_ID = "net.rwhps.server"
    /** 自定义 RELAY ID */
    const val SERVER_ID_RELAY = "net.rwhps.server.relayCustomMode.Dr"
    /** RELAY诱骗 UUID-Hex 时的 UUID */
    const val SERVER_ID_RELAY_GET = "net.rwhps.server.relayGetUUIDHex.Dr"
    /** 服务器主版本 */
    const val SERVER_CORE_VERSION = "2.3.0-M3"
    /** 服务器Topt密码  */
    const val TOPT_KEY = "net.rwhps.server.topt # RW-HPS Team"
    /** RELAY 使用的 UUID */
    const val SERVER_RELAY_UUID = "RCN Team & Tiexiu.xyz Core Team"
    /** EULA 的版本 */
    const val SERVER_EULA_VERSION = "1.1.0"


    /** 单位数据缓存  */
	@JvmField val utilData = CompressOutputStream.getGzipOutputStream("customUnits", false)

    @JvmField val LINE_SEPARATOR: String = System.getProperty("line.separator")

    /** 服务端 服务端命令  */
    @JvmField val SERVER_COMMAND = CommandHandler("")
    /** 服务端 客户端命令  */
    @JvmField val CLIENT_COMMAND = CommandHandler("/")
    /** 服务端 Log命令  */
    @JvmField val LOG_COMMAND = CommandHandler("!")
    /** 服务端 Relay命令  */
    @JvmField val RELAY_COMMAND = CommandHandler(".")

    /** Map */
    @JvmField val MapsMap = ObjectMap<String, String>()

    @JvmField val core = Application()
    @JvmField val i18NBundleMap = ObjectMap<String, I18NBundle>(8)
    @JvmField val urlData: LoadIni = LoadIni(Data::class.java.getResourceAsStream("/URL.ini")!!)

    /** 服务端 核心配置  */
    lateinit var config: BeanCoreConfig
    /** 服务端 Server配置  */
    lateinit var configServer: BeanServerConfig
    /** 服务端 Relay配置  */
    lateinit var configRelay: BeanRelayConfig

    /** 服务器默认 WebData 数据 */
    val webData = WebData()

    /**
     * 可控变量
     */
    lateinit var i18NBundle: I18NBundle
    @Deprecated("Hess 替代")
    val game: Rules by lazy {
        Rules(config, configServer).apply {
            init()
        }
    }
    @JvmField var vote: Vote? = null
    // TODO
    var bindForcibly = true

    @Volatile var startServer = false
    @Volatile var exitFlag = false

    val headlessName: String = "RW-HPS Core Headless"


    internal val privateOut = System.out
    internal lateinit var privateReader: LineReader

    @JvmField val defPrint = StrCons { obj: String -> Log.clog(obj) }

    var serverCountry = "EN"
}