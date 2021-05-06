package com.example.demo.service.connect;

import com.example.demo.config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/**
 * Netty 服务器配置
 * @author Marion
 * @date 2021/5/2
 */
@Slf4j
public class NettyServerBootStrap {

    /**
     * 创建bootstrap 是一个启动NIO服务的辅助启动类
     */
    private ServerBootstrap bootstrap = new ServerBootstrap();
    /**
     * BOSS 用来接收进来的连接 默认的线程数是2*cpu ，基于Netty主从多线程模型 ，
     *所以 主线程设置为1 ，减少资源的浪费
     */
    private EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
    /**
     * Worker 用来处理已经被接收的连接 默认的线程数是 2*cpu核心数 ,我们使用CPU核心数-1，
     减少CPU的切换，来提高性能
     */
    private EventLoopGroup workerGroup = new NioEventLoopGroup
            (Runtime.getRuntime().availableProcessors()-1);
    /**
     * Netty服务器配置类
     */
//    @Resource
    private NettyServerConfig nettyConfig;
    /**
     * 关闭服务器方法
     */
//    @PreDestroy
    public void close() {
        log.info("#########【关闭服务器】#########");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
    /**
     * 开启及服务线程
     */
//    @PostConstruct
    public void start() {
        // -- 使用单独的一个线程启动Netty，不阻塞Main线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                process();
            }
        }).start();
    }

    /**
     * netty 启动参数配置
     */
    public void process(){
        // 从配置文件中(application.yml)获取服务端监听端口号
        int port = nettyConfig.getPort();
        bootstrap.group( bossGroup, workerGroup );
        /**
         * 设置channel类型为NIO类型 , [异步的服务器端 TCP Socket 连接]
         */
        bootstrap.channel( NioServerSocketChannel.class );

        /************
         *  option 与 childOption 设置的区别
         *  option主要是针对boss线程组，child主要是针对worker线程组
         * **********/


        /**
         *BACKLOG用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，
         *用于临时存放已完成三次握手的请求的队列的最大长度 ，当前设置为64
         */
        bootstrap.option(ChannelOption.SO_BACKLOG , 64);


        //允许在同一端口上启动同一服务器的多个实例
        bootstrap.option( ChannelOption.SO_REUSEADDR, true );

        //使用ByteBuf 对象池 ，重用缓存池
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        try {
            //设置事件处理
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline p = socketChannel.pipeline();
                    //使用心跳机制，如果60S内没有回复 就触发一次userEventTrigger()方法  ,可以解决内存泄漏的问题
                    p.addLast( new IdleStateHandler( NettyServerHandler.readerIdleTime, 0, 0, TimeUnit.SECONDS ) );
                    /**
                     * 自定义协议包头示意图
                     +-----+-------+-----+-----+-----+-----+
                     | mark| ver |devType|mode|serial|length|
                     +-----+-----+-----+-----+-----+-----+
                     | 2      |    2  |        2   |      2  |    4   |     4  |
                     +-----+-----+-----+-----+-----+-----+
                     * 解析包头跟包体 ，带有长度包头的数据包
                     *  maxFrameLength : 消息的最大长度,LengthFieldBasedFrameDecoder接收的最大长度
                     *  lengthFieldOffset ：长度的偏移量
                     *  lengthFieldLength : 长度占用字节
                     *  lengthAdjustment ： 忽略多长字段 （在总长度包括包头的情况下需要输入，如果只是包体的长度 不需输入）
                     *  initialBytesToStrip : 忽略多少字节（通常是忽略包头长度）
                     */
                    final int maxFrameLength = Integer.MAX_VALUE;
                    final int lengthFieldOffset = 12;
                    final int lengthFieldLength = 4;
                    final int lengthAdjustment = 0;
                    final int initialBytesToStrip = 0;

                    //使用Netty 基于自定义包头去编解码
                    p.addLast( new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN ,maxFrameLength,
                            lengthFieldOffset,lengthFieldLength,lengthAdjustment ,initialBytesToStrip,true));

                    //由于内部使用了连接池，避免多次被实例化
//                    p.addLast( NettyServerHandler.INSTANCE );

                }
            });


            /**
             * 如果要求高实时性，有数据发送时就马上发送，就将该选项设置为true启动Nagle算法；
             * 由于我们的业务场景，数据量较少，开启NODELAY来减少延时
             */
            bootstrap.childOption(ChannelOption.TCP_NODELAY, true);


            /**
             * 使用ByteBuf 对象池
             */
            bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

            /**
             * 设置接收缓冲区的大小
             */
            bootstrap.childOption(ChannelOption.SO_RCVBUF , 1024*1024*1024 );

            /**
             * 一段时间客户端没有信息了，通过ack确认客户端是否在线
             */
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            /**
             * 设置发送缓冲区的大小
             */
            bootstrap.childOption(ChannelOption.SO_SNDBUF , 1024*1024*1024 );

            ChannelFuture f = bootstrap.bind(port).sync();
            log.info("netty服务器在【{}】端口启动监听", port);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.warn("【出现异常】 释放资源 {} " , e.getLocalizedMessage());  ;
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
