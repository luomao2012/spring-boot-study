package com.example.demo.service.connect;

import com.example.demo.config.NettyServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

/**
 * 基于Netty构建的WebSocket进入/离开/响应数据
 * @author Marion
 * @date 2021/5/2
 */
public class WebSocketBootstrap extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    private static final String WEB_SOCKET_URL = "ws://localhost:8888/websocket";

    private static final int HTTP_SUCCESS = 200;

    /**
     * 客户端与服务器建立连接时候调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyServerConfig.group.add(ctx.channel());
    }

    /**
     * 客户端与服务器断开连接时候调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        NettyServerConfig.group.remove(ctx.channel());
    }

    /**
     * 服务端接受客户端发送的数据完成之后
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 工程出现异常时候调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端websocket请求核心方法
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        /**
         * 1. 判断是否HTTP请求还是WebSocket请求
         */
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * 处理客户端向服务端发起http握手请求的业务
     */
    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        /**
         * 1. 如果不是WebSocket握手请求，则直接返回
         *      1-1. 响应是否成功
         *      2-1. Upgrade是否websocket
         * 2. 从工厂中创建实例，handshake处理chanel中的request的请求
         */

        //1. 如果不是WebSocket握手请求，则直接返回
        if (!request.decoderResult().isSuccess()
                || !"websocket".contentEquals(request.headers().get("Upgrade"))) {
            sendHttpRequest(ctx, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        //2. 从工厂中创建实例，handshake处理chanel中的request的请求
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
        }
    }

    /**
     * 处理WebSocket请求
     */
    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        /**
         * 1. 验证消息类型close/ping，目前只支持Text处理
         * 2. 处理接受到数据
         * 3. 广播给所有channel
         */

        // 1. 验证消息类型close/ping，目前只支持Text处理
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
        }

        if (frame instanceof PingWebSocketFrame) {
            System.out.println("暂不支持二进制消息");
            ctx.channel().write(new PingWebSocketFrame(frame.content().retain()));
            return;
        }

        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("暂不支持二进制消息");
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持消息");
        }

        // 2. 处理接受到数据
        String text = ((TextWebSocketFrame) frame).text();
        System.out.println("收到文本数据" + text);

        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString()
                + ctx.channel().id()
                + " : "
                + text);

        // 3. 广播给所有channel
        NettyServerConfig.group.writeAndFlush(tws);
    }

    /**
     * 服务端向客户端响应消息
     */
    private void sendHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request, DefaultFullHttpResponse response) {
        /**
         * 1. 如果响应不是200，则关闭资源
         * 2. 服务端向客户端发送数据
         */
        if (response.status().code() != HTTP_SUCCESS) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(byteBuf);
            byteBuf.release();
        }

        //2. 服务端向客户端发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);

        if (response.status().code() != HTTP_SUCCESS) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

}
