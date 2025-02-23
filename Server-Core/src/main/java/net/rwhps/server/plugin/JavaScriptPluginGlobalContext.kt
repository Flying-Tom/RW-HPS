/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin

import net.rwhps.server.data.bean.BeanPluginInfo
import net.rwhps.server.data.event.GameOverData
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.PlayerHess
import net.rwhps.server.func.ConsMap
import net.rwhps.server.func.ConsSeq
import net.rwhps.server.func.Prov
import net.rwhps.server.game.GameMaps
import net.rwhps.server.game.GameUnitType
import net.rwhps.server.game.simulation.core.AbstractPlayerData
import net.rwhps.server.io.input.SyncByteArrayChannel
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.output.DisableSyncByteArrayOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.GroupNet
import net.rwhps.server.net.HttpRequestOkHttp
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.core.server.AbstractNetConnectServer
import net.rwhps.server.struct.IntMap
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.I18NBundle
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.RandomUtils
import net.rwhps.server.util.compression.core.AbstractDecoder
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.inline.ifNullResult
import net.rwhps.server.util.io.IOUtils
import net.rwhps.server.util.log.Log
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.FileSystem
import java.io.*
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.*
import kotlin.io.path.Path

/**
 * 插件全局上下文
 * 主要用于JS插件加载
 *
 * @author RW-HPS/ZeroDegress
 * @author RW-HPS/Dr
 */
class JavaScriptPluginGlobalContext {
    private val moduleMap = ObjectMap<String, Path>()
    private val javaMap = ObjectMap<String, Path>()
    private val scriptFileSystem = OrderedMap<String, ByteArray>()
    private val modules = ObjectMap<BeanPluginInfo, String>()
    private val fakeFileSystem = FakeFileSystem()
    private val truffleFileSystem = getOnlyReadFileSystem(this.fakeFileSystem)
    private val rwhpsObject = RwHpsJS(this)
    class RwHpsJS(
        private val context: JavaScriptPluginGlobalContext
    ) {
        fun readRamBytes(path: String): ByteArray? {
            return if(path.startsWith("/")) {
                context.scriptFileSystem[path]
            } else {
                context.scriptFileSystem[context.truffleFileSystem.parsePath(URI(path)).toString()]
            }
        }

        fun readRamText(path: String): String? {
            return if(path.startsWith("/")) {
                context.scriptFileSystem[path]
            } else {
                context.scriptFileSystem[context.truffleFileSystem.parsePath(URI(path)).toString()]
            }?.decodeToString()
        }
    }

    init {
        injectJavaClass<AbstractDecoder>()
        injectJavaClass<AbstractNetConnectServer>()
        injectJavaClass<PlayerHess>()
        injectJavaClass<AbstractPlayerData>()
        injectJavaClass<ConnectionAgreement>()
        injectJavaClass<CompressOutputStream>()
        injectJavaClass<CommandHandler>()
        injectJavaClass<CommandHandler.Command>("Command")
        injectJavaClass<CommandHandler.CommandResponse>("CommandResponse")
        injectJavaClass<CommandHandler.CommandRunner<Any>>("CommandRunner")
        injectJavaClass<CommandHandler.ResponseType>("ResponseType")
        injectJavaClass<ConsMap<Any, Any>>()
        injectJavaClass<ConsSeq<Any>>()
        injectJavaClass<DisableSyncByteArrayOutputStream>()
        injectJavaClass<DataPermissionStatus.ServerStatus>("ServerStatus")
        injectJavaClass<File>()
        injectJavaClass<FileOutputStream>()
        injectJavaClass<FileUtils>()
        injectJavaClass<GameMaps.MapData>("MapData")
        injectJavaClass<GameOverData>()
        injectJavaClass<GameUnitType.GameActions>("GameActions")
        injectJavaClass<GameUnitType.GameUnits>("GameUnits")
        injectJavaClass<GroupNet>()
        injectJavaClass<I18NBundle>()
        injectJavaClass<IntMap<Any>>()
        injectJavaClass<InputStream>()
        injectJavaClass<InputStreamReader>()
        injectJavaClass<IRwHps.NetType>("NetType")
        injectJavaClass<ObjectMap<Any, Any>>()
        injectJavaClass<OrderedMap<Any, Any>>()
        injectJavaClass<OutputStream>()
        injectJavaClass<Packet>()
        injectJavaClass<PacketType>()
        injectJavaClass<Plugin>()
        injectJavaClass<GetVersion>()
        injectJavaClass<Properties>()
        injectJavaClass<Prov<Any>>()
        injectJavaClass<Base64>()
        injectJavaClass<Log>()
        injectJavaClass<ByteBuffer>()
    }

    /**
     * 注册一个模块，这样在js中可通过"@module"这样的方式引用
     *
     * @param name 模块名称
     * @param main 模块入口文件
     */
    fun registerModule(name: String, main: Path) {
        Log.debug("Registered Module to esm: $name")
        moduleMap[name] = main
    }

    /**
     * 注册一个Java包，这样在js中可通过"java:package"这样的方式引用
     *
     * @param name 包名称
     * @param main 模块入口文件
     */
    fun registerJavaPackage(name: String, main: Path) {
        Log.debug("Registered Java package to esm: $name")
        javaMap[name] = main
    }

    /**
     * 添加一个ESM插件
     *
     * @param pluginInfo ESM插件JSON数据
     * @param pluginData ESM插件模块所在压缩包的Map格式
     */
    fun addESMPlugin(pluginInfo: BeanPluginInfo, pluginData: OrderedMap<String, ByteArray>) {
        val pluginName = pluginInfo.name
        val mainPath = this.fakeFileSystem.getPath("/plugins", pluginName, pluginInfo.main)
        registerModule(pluginName, mainPath)

        pluginData.eachAll { k,v ->
            val path = fakeFileSystem.getPath("/plugins", pluginName, k)
            scriptFileSystem[path.toString()] = v
            fakeFileSystem.addFile(path)
        }

        modules[pluginInfo, RandomUtils.getRandomIetterString(10)]
    }

    /**
     * 加载全部ESM插件
     *
     * @return 插件加载数据
     */
    fun loadESMPlugins(): Seq<PluginLoadData> {
        try {
            val cx = Context.newBuilder()
                .allowExperimentalOptions(true)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .option("js.esm-eval-returns-exports", "true")
                .option("js.webassembly", "true")
                .allowHostAccess(HostAccess.newBuilder()
                    .allowAllClassImplementations(true)
                    .allowAllImplementations(true)
                    .allowPublicAccess(true)
                    .allowArrayAccess(true)
                    .allowListAccess(true)
                    .allowIterableAccess(true)
                    .allowIteratorAccess(true)
                    .allowMapAccess(true)
                    .build())
                .allowHostClassLookup { _ -> true }
                .fileSystem(this.truffleFileSystem)
                .allowIO(true)
                .build()
            cx.getBindings("js")
                .putMember("RwHps", rwhpsObject)
            cx.enter()

            var loadScript = ""
            modules.eachAll { pluginInfo,v ->
                loadScript += "export { default as $v } from '${pluginInfo.name}';"
                loadScript += Data.LINE_SEPARATOR
            }

            val defaults = cx.eval(Source.newBuilder("js", loadScript, "/work/load.mjs").build())

            return Seq<PluginLoadData>().apply {
                modules.eachAll { pluginInfo,v ->
                    this.add(PluginLoadData(
                        pluginInfo.name,
                        pluginInfo.author,
                        pluginInfo.description,
                        pluginInfo.version,
                        if(defaults.canExecute()) {
                            defaults.getMember(v).execute().`as`(Plugin::class.java)
                        } else {
                            defaults.getMember(v).`as`(Plugin::class.java)
                        }
                    ))
                }
            }
        } catch (e: Exception) {
            Log.error("JavaScript plugin loading failed: ", e)
        }
        return Seq()
    }

    /**
     * 将指定Java类型注入
     */
    private inline fun <reified T> injectJavaClass(rename: String? = null) {
        val packageName = T::class.java.`package`.name
        val packagePathAll = "/java/$packageName/index.mjs"

        if(!javaMap.containsKey(packageName)) {
            registerJavaPackage(packageName, Path(packagePathAll))
            scriptFileSystem[packagePathAll] = """
                export const ${rename ?: T::class.java.name.split(".").last()} = Java.type('${T::class.java.name}');
            """.trimIndent().toByteArray()
            return
        }

        scriptFileSystem[packagePathAll] = """
            ${String(scriptFileSystem[packagePathAll]!!)}
            export const ${rename ?: T::class.java.name.split(".").last()} = Java.type('${T::class.java.name}');
        """.trimIndent().toByteArray()
    }

    /**
     * RamFileSystem
     *
     * @return FileSystem
     */
    private fun getOnlyReadFileSystem(
        fakeFileSystem: java.nio.file.FileSystem
    ): FileSystem {
        return object: FileSystem {
            override fun parsePath(uri: URI?): Path {
                if(uri == null) {
                    return fakeFileSystem.getPath("/null")
                }
                var toPath = ""
                when(uri.scheme) {
                    "ram" -> toPath += uri.path
                    "plugin" -> {
                        toPath += "/plugins/${uri.host}"
                        if(uri.path.isBlank()) {
                            toPath = moduleMap[uri.host].toString()
                        }
                    }
                    "java" -> toPath += "/java/${uri.host}${uri.path}"
                    "http" -> toPath += "/web/http/${uri.authority}${uri.path}"
                    "https" -> toPath += "/web/https/${uri.authority}${uri.path}"
                    else -> parsePath(uri.rawPath)
                }
                if(uri.query != null) {
                    toPath = toPath.removeSuffix("/") + "/?${uri.query}"
                }
                if(uri.fragment != null) {
                    toPath = toPath.removeSuffix("/") + "/#${uri.fragment}"
                }
                return fakeFileSystem.getPath(toPath)
            }

            override fun parsePath(path: String?): Path {
                if (path == null) {
                    return fakeFileSystem.getPath("/null")
                }
                return if(path.startsWith("../") || path.startsWith("./") || path.startsWith("/")) {
                    fakeFileSystem.getPath(path)
                } else {
                    try {
                        parsePath(URI("plugin://$path"))
                    } catch (e: java.net.URISyntaxException) {
                        fakeFileSystem.getPath("/null")
                    }
                }
            }

            /**
             * checkAccess Ignored, Because the RAM File System does not need it
             * @param path Path
             * @param modes MutableSet<out AccessMode>
             * @param linkOptions Array<out LinkOption?>
             */
            override fun checkAccess(path: Path?, modes: MutableSet<out AccessMode>?, vararg linkOptions: LinkOption?) {
                // Ignored, because the RAM File System does not need it
            }
            /**
             * createDirectory Ignored, Because the RAM File System does not need it
             * @param dir Path
             * @param attrs Array<out FileAttribute<*>?>
             */
            override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) {
                // Ignored, because the RAM File System does not need it
            }
            /**
             * delete Ignored, Because the RAM File System does not need it
             * @param path Path
             * @return Path
             */
            override fun delete(path: Path?) {
                // Ignored, because the RAM File System does not need it
            }

            override fun newByteChannel(path: Path, options: MutableSet<out OpenOption>?, vararg attrs: FileAttribute<*>?): SeekableByteChannel {
                var pathString = path.toString()
                val reg = Regex("^(.+?)(/\\?[^?#]+)?(/#[^#]+)?\$")
                val res = reg.matchEntire(pathString)
                var query = ""
                var fragment = ""
                if(res != null) {
                    pathString = res.groupValues[1]
                    query = res.groupValues[2].removePrefix("/?")
                    fragment = res.groupValues[3].removePrefix("/#")
                }
                val bytes = when {
                    pathString.startsWith("/web") -> {
                        val webReg = Regex("^/web/(https|http)(.+?)(/\\?[^?#]+)?(/#[^#]+)?\$")
                        val webRes = webReg.matchEntire(pathString)
                        if(webRes != null) {
                            val list = webRes.groupValues
                            val uri = "${list[1]}://${list[2]}${list[3].removePrefix("/")}${list[4].removePrefix("/")}"
                            HttpRequestOkHttp.doGet(uri).toByteArray()
                        } else {
                            null
                        }
                    }
                    pathString.matches(Regex("^/plugins/[^/]+\$")) -> scriptFileSystem[
                        moduleMap[pathString.removePrefix("/plugins/")].toString()
                    ]
                    else -> scriptFileSystem[pathString]
                }
                return SyncByteArrayChannel(when {
                    pathString.startsWith("/web") -> bytes
                    fragment.isBlank() -> when {
                        query.isBlank() -> bytes
                        query == "bytes" -> """
                            const bytes = new Uint8Array(RwHps.readRamBytes('$pathString'));
                            export default bytes;
                            export const url = 'ram://$pathString';
                            export const type = 'bytes';
                        """.trimIndent().encodeToByteArray()
                        query == "text" -> """
                            const text = RwHps.readRamText('$pathString');
                            export default text;
                            export const url = 'ram://$pathString';
                            export const type = 'text';
                        """.trimIndent().encodeToByteArray()
                        query == "json" -> """
                            const json = JSON.parse(RwHps.readRamText('$pathString'));
                            export default json;
                            export const url = 'ram://$pathString';
                            export const type = 'json';
                        """.trimIndent().encodeToByteArray()
                        query == "wasm" -> """
                            const wasm = WebAssembly.instantiate(new Uint8Array(RwHps.readRamBytes('$pathString')),{});
                            export default wasm
                            export const url = 'ram://$pathString';
                            export const type = 'wasm';
                        """.trimIndent().encodeToByteArray()
                        else -> null
                    }
                    else -> null
                }.ifNullResult(IOUtils.EMPTY_BYTE_ARRAY) { it },true)
            }

            override fun newDirectoryStream(dir: Path?, filter: DirectoryStream.Filter<in Path>?): DirectoryStream<Path> {
                Log.debug("G newDirectoryStream")
                TODO("Not yet implemented")
            }

            /**
             * toAbsolutePath Ignored, Because the RAM File System does not need it
             * @param path Path
             * @return Path
             */
            override fun toAbsolutePath(path: Path): Path {
                return path.toAbsolutePath()
            }
            /**
             * toRealPath Ignored, Because the RAM File System does not need it
             * @param path Path
             * @return Path
             */override fun toRealPath(path: Path, vararg linkOptions: LinkOption?): Path {
                return path.toRealPath()
            }

            override fun readAttributes(path: Path?, attributes: String?, vararg options: LinkOption?): MutableMap<String, Any> {
                TODO()
            }
        }
    }
}