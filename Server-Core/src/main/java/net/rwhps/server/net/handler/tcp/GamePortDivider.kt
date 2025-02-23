/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.handler.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import net.rwhps.server.net.http.SendWeb
import net.rwhps.server.net.http.WebData
import net.rwhps.server.net.http.WebData.Companion.WS_URI
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.exp.ExceptionX
import java.nio.charset.StandardCharsets


/**
 * 根据第一个包来判定连接
 * @author HuiAnxiaoxing
 * @author RW-HPS/Dr
 */
@ChannelHandler.Sharable
internal class GamePortDivider(private val divider: StartGamePortDivider) : ChannelInboundHandlerAdapter() {
    private lateinit var webData: WebData

    fun setWebData(data: WebData) {
        webData = data
    }

    private fun isHttpReq(head: String): Boolean {
        return head.startsWith("GET ") || head.startsWith("POST ") || head.startsWith("DELETE ") || head.startsWith("HEAD ") || head.startsWith("PUT ")
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val firstData: ByteBuf = msg as ByteBuf

        val headS: String = firstData.toString(StandardCharsets.UTF_8)

        if (isHttpReq(headS)) {
            if (headS.startsWith("GET $WS_URI")) {
                val head = headS.split("\\R")[0].split(" ")[1]
                ctx.pipeline().addLast(
                    IdleStateHandler(10, 0, 0), object : ChannelDuplexHandler() {
                        @Throws(Exception::class)
                        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                            val evt1: IdleStateEvent = evt as IdleStateEvent
                            if (evt1.state() === IdleState.READER_IDLE) {
                                Log.clog("已经10秒没有读到数据了,主动断开连接" + ctx.channel())
                                ctx.channel().close()
                            }
                        }
                    },
                    HttpServerCodec(),
                    ChunkedWriteHandler(),
                    HttpObjectAggregator(1048576),
                    WebSocketServerProtocolHandler(head)
                )
                val wsProcessing = webData.runWebSocketInstance(head)
                if (wsProcessing != null) {
                    ctx.channel().pipeline().addLast(wsProcessing)
                } else {
                    ctx.close()
                }
            } else {
                ctx.channel().pipeline().addLast(HttpServerCodec(), HttpObjectAggregator(1048576))
                ctx.channel().pipeline().addLast(object : SimpleChannelInboundHandler<Any>() {
                    @Throws(Exception::class)
                    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
                        if (msg is HttpRequest) {
                            val request = msg

                            val url = request.uri()

                            if (request.method().equals(HttpMethod.GET)) {
                                webData.runWebGetInstance(url, request, SendWeb(ctx.channel(), request))
                                return
                            } else if (request.method().equals(HttpMethod.POST)) {
                                if (msg is HttpContent) {
                                    val httpContent = msg as HttpContent
                                    val content = httpContent.content()
                                    val buf = StringBuilder()
                                    buf.append(content.toString(CharsetUtil.UTF_8))
                                    webData.runWebPostInstance(url, buf.toString(), request, SendWeb(ctx.channel(), request))
                                    return
                                }
                            }
                        }
                    }
                })
            }
        } else {
            divider.resetGameProtocol()
        }
        ctx.pipeline().remove("GamePortDivider")
        super.channelRead(ctx, msg)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        cause?.let {
            Log.error(ExceptionX.resolveTrace(it))
        }
    }
}