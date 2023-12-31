package com.example.ModbusClient.config.netty;

import com.example.ModbusClient.config.netty.handler.NettyChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NettyConfig {

    @Bean
    public Bootstrap readBootstrap(NettyChannelInitializer nettyChannelInitializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group()).channel(NioSocketChannel.class).handler(nettyChannelInitializer);

        return bootstrap;
    }


    @Bean(destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup group() {
        return new NioEventLoopGroup();
    }

}
