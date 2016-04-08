package com.patterncat.rpc.client;

import com.google.common.base.Optional;
import com.patterncat.rpc.dto.RpcRequest;
import com.patterncat.rpc.dto.RpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by patterncat on 2016/4/6.
 */
@ChannelHandler.Sharable
public class ClientRpcHandler extends SimpleChannelInboundHandler<RpcResponse> {

    //用blocking queue主要是用阻塞的功能，省的自己加锁
    private final ConcurrentHashMap<String, BlockingQueue<RpcResponse>> responseMap = new ConcurrentHashMap<String, BlockingQueue<RpcResponse>>();


    //messageReceived
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        System.out.println("receive response:"+rpcResponse);
        BlockingQueue<RpcResponse> queue = responseMap.get(rpcResponse.getTraceId());
        queue.add(rpcResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }

    public RpcResponse send(RpcRequest request,Optional<Pair<Long,TimeUnit>> timeout) throws InterruptedException {
        responseMap.putIfAbsent(request.getTraceId(), new LinkedBlockingQueue<RpcResponse>(1));
        RpcResponse response = null;
        try {
            BlockingQueue<RpcResponse> queue = responseMap.get(request.getTraceId());
            if(timeout == null || !timeout.isPresent()){
                response = queue.take();
            }else{
                response = queue.poll(timeout.get().getKey(),timeout.get().getValue());
            }
        } finally {
            responseMap.remove(request.getTraceId());
        }
        return response;
    }
}