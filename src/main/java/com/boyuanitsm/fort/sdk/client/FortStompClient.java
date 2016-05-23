package com.boyuanitsm.fort.sdk.client;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.boyuanitsm.fort.sdk.bean.OnUpdateSecurityResource;
import com.boyuanitsm.fort.sdk.cache.FortResourceCache;
import com.boyuanitsm.fort.sdk.config.FortConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * fort stomp client. subscribe on update security resource.
 *
 * @author zhanghua on 5/22/16.
 */
@Component
public class FortStompClient {

    private final Logger log = LoggerFactory.getLogger(FortClient.class);

    @Autowired
    private FortConfiguration configuration;

    @Autowired
    private FortResourceCache cache;

    @Autowired
    public FortStompClient(FortClient client, FortConfiguration configuration) {
        this.configuration = configuration;

        List<Transport> transports = new ArrayList<Transport>(2);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", client.getCookieString());

        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        ListenableFuture<StompSession> future = stompClient.connect(configuration.getApp().getWebsocketServerBase() + "/websocket/sa", headers, new MyWebSocketHandler());

        future.addCallback(new SuccessCallback<StompSession>() {
            public void onSuccess(StompSession stompSession) {
                log.info("login fort web socket stomp server success!");
            }
        }, new FailureCallback() {
            public void onFailure(Throwable throwable) {
                log.info("login fort web socket stomp server failure!");
            }
        });
    }

    private class MyWebSocketHandler implements StompSessionHandler {


        public void afterConnected(StompSession stompSession, StompHeaders stompHeaders) {
            stompSession.subscribe(String.format("/topic/%s/onUpdateSecurityResource", configuration.getApp().getAppKey()), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders stompHeaders) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders stompHeaders, Object o) {
                    OnUpdateSecurityResource onUpdateSecurityResource = JSON.toJavaObject(JSONObject.parseObject(o.toString()), OnUpdateSecurityResource.class);
                    cache.updateResource(onUpdateSecurityResource);
                }
            });
        }

        public void handleException(StompSession stompSession, StompCommand stompCommand, StompHeaders stompHeaders, byte[] bytes, Throwable throwable) {

        }

        public void handleTransportError(StompSession stompSession, Throwable throwable) {

        }

        public Type getPayloadType(StompHeaders stompHeaders) {
            return null;
        }

        public void handleFrame(StompHeaders stompHeaders, Object o) {
        }
    }

}
