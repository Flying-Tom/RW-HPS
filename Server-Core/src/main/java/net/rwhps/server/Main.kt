/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

/**
 * 声明 :
 * 一切开发旨在学习，请勿用于非法用途
 * RW-HPS 是完全免费且开放源代码的软件，仅供学习和娱乐用途使用
 * RW-HPS 不会通过任何方式强制收取费用，或对使用者提出物质条件
 * RW-HPS 由整个开源社区维护，并不是属于某个个体的作品，所有贡献者都享有其作品的著作权
 *
 * 若修改 RW-HPS 源代码再发布，或参考 RW-HPS 内部实现
 * 则衍生项目必须在文章首部或 'RW-HPS' 相关内容首次出现的位置明确声明来源于本仓库 (https://github.com/RW-HPS/RW-HPS) 不得扭曲或隐藏免费且开源的事实
 */
/**
 * Disclaimer :
 * All development is for learning purposes only, please do not use it for illegal purposes
 * RW-HPS is completely free and open source software for learning and entertainment purposes only
 * RW-HPS does not impose fees or material conditions on users in any way.
 * RW-HPS is maintained by the entire open source community and is not the work of a single individual, and all contributors own the copyright to their work
 *
 * The RW-HPS is maintained by the entire open source community and is not the work of any individual.
 * If you modify the RW-HPS source code for redistribution, or refer to the RW-HPS internal implementation
 * Derivative projects must explicitly state that they are from this repository (https://github.com/RW-HPS/RW-HPS) at the beginning of the article or at the first appearance of the 'RW-HPS' related content, without distorting or hiding the fact that they are free and open source
 */

package net.rwhps.server

import net.rwhps.server.command.CoreCommands
import net.rwhps.server.command.LogCommands
import net.rwhps.server.core.Initialization
import net.rwhps.server.core.thread.Threads.newThreadCore
import net.rwhps.server.custom.LoadCoreCustomPlugin
import net.rwhps.server.data.bean.BeanCoreConfig
import net.rwhps.server.data.bean.BeanRelayConfig
import net.rwhps.server.data.bean.BeanServerConfig
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.Data.privateReader
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.data.plugin.PluginManage.addGlobalEventManage
import net.rwhps.server.data.plugin.PluginManage.init
import net.rwhps.server.data.plugin.PluginManage.loadSize
import net.rwhps.server.data.plugin.PluginManage.runInit
import net.rwhps.server.data.plugin.PluginManage.runOnEnable
import net.rwhps.server.data.plugin.PluginManage.runRegisterGlobalEvents
import net.rwhps.server.data.totalizer.TimeAndNumber
import net.rwhps.server.dependent.HeadlessProxyClass
import net.rwhps.server.game.EventGlobal
import net.rwhps.server.game.event.global.ServerLoadEvent
import net.rwhps.server.io.ConsoleStream
import net.rwhps.server.io.output.DynamicPrintStream
import net.rwhps.server.net.NetService
import net.rwhps.server.net.api.WebGetRelayInfo
import net.rwhps.server.net.handler.tcp.StartHttp
import net.rwhps.server.net.http.WebData
import net.rwhps.server.util.CLITools
import net.rwhps.server.util.SystemSetProperty
import net.rwhps.server.util.file.FileUtils.Companion.getFolder
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.clog
import net.rwhps.server.util.log.Log.set
import net.rwhps.server.util.log.Log.setCopyPrint
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.InterruptedIOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess


/**
 * Welcome to Bugs RW-HPS !
 * Welcome to the RW-HPS Team !
 */

/**
 * @author RW-HPS/Dr
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        /* 设置Log 并开启拷贝 */
        set("WARN")
        setCopyPrint(true)

        /* OFF WARN */
        System.setProperty("org.jline.terminal.dumb", "true")
        Logger.getLogger("io.netty").level = Level.OFF

        /* Fix Idea */
        System.setProperty("jansi.passthrough", "true")
        /* 覆盖输入输出流 */
        inputMonitorInit()

        SystemSetProperty.setOnlyIpv4()
        SystemSetProperty.setAwtHeadless()

        Initialization()

        clog(Data.i18NBundle.getinput("server.login"))
        clog("Load ing...")

        Data.config = BeanCoreConfig.stringToClass()
        Data.configServer = BeanServerConfig.stringToClass()

        Data.configRelay = BeanRelayConfig.stringToClass()

        Data.core.load()
        Initialization.loadLib()

        clog(Data.i18NBundle.getinput("server.hi"))
        clog(Data.i18NBundle.getinput("server.project.url"))
        clog(Data.i18NBundle.getinput("server.thanks"))

        // Test Block
        run {
        }

        /* 加载 ASM */
        HeadlessProxyClass()

        /* 命令加载 */
        CoreCommands(Data.SERVER_COMMAND)
        LogCommands(Data.LOG_COMMAND)
        clog(Data.i18NBundle.getinput("server.load.command"))

        /* Event加载 */
        addGlobalEventManage(EventGlobal())

        clog(Data.i18NBundle.getinput("server.load.events"))

        /* 初始化Plugin */
        init(getFolder(Data.Plugin_Plugins_Path))
        LoadCoreCustomPlugin()
        runOnEnable()
        runRegisterGlobalEvents()
        PluginManage.runRegisterCoreCommands(Data.SERVER_COMMAND)
        PluginManage.runRegisterServerCommands(Data.SERVER_COMMAND)
        PluginManage.runRegisterServerClientCommands(Data.CLIENT_COMMAND)

        /* 加载完毕 */
        PluginManage.runGlobalEventManage(ServerLoadEvent())

        /* 初始化Plugin Init */
        runInit()
        clog(Data.i18NBundle.getinput("server.loadPlugin", loadSize))

        set(Data.config.Log)

        /* 默认直接启动服务器 */
        val response = Data.SERVER_COMMAND.handleMessage("start", Data.defPrint)
        if (response != null && response.type != CommandHandler.ResponseType.noCommand && response.type != CommandHandler.ResponseType.valid) {
            clog("Please check the command , Unable to use StartCommand inside Config to start the server")
        }

        if (Data.config.CmdTitle.isBlank()) {
            CLITools.setWindowsCmdTitle("[RW-HPS] Port: ${Data.config.Port}, Run Server: ${NetStaticData.ServerNetType.name}")
        } else {
            CLITools.setWindowsCmdTitle(Data.config.CmdTitle)
        }

        newThreadCore {
            Data.webData.addWebGetInstance("/api/getRelayInfo", WebGetRelayInfo())
            NetService(StartHttp::class.java).openPort(5000)
        }

        newThreadCore(this::inputMonitor)
    }

    /**
     * 摆烂
     * Win的CMD就是个垃圾
     */
    private fun inputMonitorInit() {
        val terminal = TerminalBuilder.builder()
            .encoding(Data.DefaultEncoding)
            .build()

        privateReader = LineReaderBuilder.builder().terminal(terminal).completer(ConsoleStream.TabCompleter).build() as LineReader

        System.setErr(DynamicPrintStream {
            Log.debug(it)
        })

        System.setOut(DynamicPrintStream {
            privateReader.printAbove(it)
        })
    }

    private fun inputMonitor() {
        //# 209 防止连续多次错误
        val idlingCount = TimeAndNumber(5, 10)
        var last = 0

        while (true) {
            val line = try {
                privateReader.readLine("> ").also {
                    last = 0
                }
            } catch (e: InterruptedIOException) {
                continue
            } catch (e: UserInterruptException) {
                if (last != 1) {
                    privateReader.printAbove("Interrupt again to force exit application")
                    last = 1
                    continue
                }
                privateReader.printAbove("force exit")
                exitProcess(255)
            } catch (e: EndOfFileException) {
                if (last != 2) {
                    privateReader.printAbove("Catch EndOfFile, again to exit application")
                    last = 2
                    continue
                }
                privateReader.printAbove("exit")
                exitProcess(1)
            }

            if (line.isEmpty()) {
                continue
            }

            try {
                val response = Data.SERVER_COMMAND.handleMessage(line, Data.defPrint)
                if (response != null && response.type != CommandHandler.ResponseType.noCommand && response.type != CommandHandler.ResponseType.valid) {
                    val text = when (response.type) {
                        CommandHandler.ResponseType.manyArguments -> {
                            "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                        }
                        CommandHandler.ResponseType.fewArguments -> {
                            "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                        }
                        else -> {
                            "Unknown command. Check help"
                        }
                    }
                    clog(text)
                }
            } catch (e: Exception) {
                if (idlingCount.checkStatus()) {
                    idlingCount.count++
                    clog("InputMonitor Error")
                    Log.error(e)
                } else {
                    clog("InputMonitor Idling")
                    return
                }
            }
        }
    }
}

// 你的身体是为你服务的 而不是RW-HPS(?)  !
/**
                        ::
                      :;J7, :,                        ::;7:
                      ,ivYi, ,                       ;LLLFS:
                      :iv7Yi                       :7ri;j5PL
                     ,:ivYLvr                    ,ivrrirrY2X,
                    :;r@Wwz.7r:                :ivu@kexianli.
                    :iL7::,:::iiirii:ii;::::,,irvF7rvvLujL7ur
                   ri::,:,::i:iiiiiii:i:irrv177JX7rYXqZEkvv17
                ;i:, , ::::iirrririi:i:::iiir2XXvii;L8OGJr71i
              :,, ,,:   ,::ir@mingyi.irii:i:::j1jri7ZBOS7ivv,
                 ,::,    ::rv77iiiriii:iii:i::,rvLq@huhao.Li
             ,,      ,, ,:ir7ir::,:::i;ir:::i:i::rSGGYri712:
           :::  ,v7r:: ::rrv77:, ,, ,:i7rrii:::::, ir7ri7Lri
          ,     2OBBOi,iiir;r::        ,irriiii::,, ,iv7Luur:
        ,,     i78MBBi,:,:::,:,  :7FSL: ,iriii:::i::,,:rLqXv::
        :      iuMMP: :,:::,:ii;2GY7OBB0viiii:i:iii:i:::iJqL;::
       ,     ::::i   ,,,,, ::LuBBu BBBBBErii:i:i:i:i:i:i:r77ii
      ,       :       , ,,:::rruBZ1MBBqi, :,,,:::,::::::iiriri:
     ,               ,,,,::::i:  @arqiao.       ,:,, ,:::ii;i7:
    :,       rjujLYLi   ,,:::::,:::::::::,,   ,:i,:,,,,,::i:iii
    ::      BBBBBBBBB0,    ,,::: , ,:::::: ,      ,,,, ,,:::::::
    i,  ,  ,8BMMBBBBBBi     ,,:,,     ,,, , ,   , , , :,::ii::i::
    :      iZMOMOMBBM2::::::::::,,,,     ,,,,,,:,,,::::i:irr:i:::,
    i   ,,:;u0MBMOG1L:::i::::::  ,,,::,   ,,, ::::::i:i:iirii:i:i:
    :    ,iuUuuXUkFu7i:iii:i:::, :,:,: ::::::::i:i:::::iirr7iiri::
    :     :rk@Yizero.i:::::, ,:ii:::::::i:::::i::,::::iirrriiiri::,
     :      5BMBBBBBBSr:,::rv2kuii:::iii::,:i:,, , ,,:,:i@petermu.,
          , :r50EZ8MBBBBGOBBBZP7::::i::,:::::,: :,:,::i;rrririiii::
              :jujYY7LS0ujJL7r::,::i::,::::::::::::::iirirrrrrrr:ii:
           ,:  :@kevensun.:,:,,,::::i:i:::::,,::::::iir;ii;7v77;ii;i,
           ,,,     ,,:,::::::i:iiiii:i::::,, ::::iiiir@xingjief.r;7:i,
        , , ,,,:,,::::::::iiiiiiiiii:,:,:::::::::iiir;ri7vL77rrirri::
         :,, , ::::::::i:::i:::i:i::,,,,,:,::i:i:::iir;@Secbone.ii:::
*/
// 音无结弦之时，悦动天使之心；立于浮华之世，奏响天籁之音