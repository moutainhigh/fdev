package com.spdb.fdev.fdevfootprint.spdb.netty.handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.spdb.fdev.fdevfootprint.base.dict.Constants;
import com.spdb.fdev.fdevfootprint.base.dict.Dict;
import com.spdb.fdev.fdevfootprint.base.utils.CommonUtil;
import com.spdb.fdev.fdevfootprint.spdb.entity.SwitchModel;
import com.spdb.fdev.fdevfootprint.spdb.service.SwitchFlagQueryService;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
@Service(Dict.HTTP_SWITCH_SERVER_INB_HANDLER)
public class HttpSwitchServerInbHandler extends ChannelInboundHandlerAdapter {
	protected Logger logger = LoggerFactory.getLogger(HttpSwitchServerInbHandler.class);
	
	@Autowired
	private SwitchFlagQueryService switchFlagQuery;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		String channel = "08";
		String ip = "";
		String host = "";
		HttpRequest request = null;
		if (msg instanceof HttpRequest) {
			request = (FullHttpRequest) msg;
			
			// ???????????????User-Agent??????
			String userAgent = request.headers().get(Constants.USER_AGENT);
	        MDC.put("User_Agent", "UserAgent:" + userAgent);
	        
	        host = request.headers().get(Dict.HOST);
	        
	        // ???????????????IP
	        ip = CommonUtil.getIpAddr(request, ctx.channel().remoteAddress());
	        MDC.put("IP", "IP:" + ip);
			
	        // ??????????????????
			QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
			Map<String, List<String>> parameters = decoder.parameters();
			List<String> sysNoPara = parameters.get(Dict.CHANNEL);
			if (sysNoPara != null && sysNoPara.size() > 0) {
				channel = parameters.get(Dict.CHANNEL).get(0);
			}
			MDC.put("CHANNEL", "CHANNEL:" + channel);
		}

		if (msg instanceof HttpContent) {
			SwitchModel switchModel = switchFlagQuery.getSwitchFlag(channel);
			JSONObject jsonObject = new JSONObject();
			boolean commonSwitch = CommonUtil.checkHandSwitchViaIP(ip, switchModel);
			jsonObject.put(Dict.SWITCH, commonSwitch);
			
			//????????????IP?????????????????????????????????????????????
//			boolean autoSwitch = CommonUtil.checkAutoSwitchViaIP(newFootDomain, ip, host, switchModel);
			jsonObject.put(Dict.AUTO_SWITCH, true);
			
			String res = jsonObject.toJSONString();
			FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(res.getBytes("UTF-8")));
			response.headers().set(CONTENT_TYPE, Constants.TEXT_JSON);

			if (HttpHeaders.isKeepAlive(request)) {
				response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
				response.headers().set(CONNECTION, Values.KEEP_ALIVE);
				ctx.writeAndFlush(response);
			} else {
				response.headers().set(CONNECTION, Values.CLOSE);
				ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
			}
			logger.info("sw request, res"+res);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error(cause.getMessage(), cause);
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			ctx.close();
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}
}
