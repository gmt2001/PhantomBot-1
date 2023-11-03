/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.httpwsserver;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;
import org.json.JSONStringer;

import com.gmt2001.httpwsserver.auth.WsAuthenticationHandler;
import com.gmt2001.wspinger.WSServerPinger;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.util.AttributeKey;

/**
 * Processes WebSocket frames and passes successful ones to the appropriate registered final handler
 *
 * @author gmt2001
 */
@Sharable
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    /**
     * A map of registered {@link WsFrameHandler} for handling WebSockets
     */
    static Map<String, WsFrameHandler> wsFrameHandlers = new ConcurrentHashMap<>();
    /**
     * Represents the {@code ATTR_URI} attribute, which stores the URI the client has connected to
     */
    public static final AttributeKey<String> ATTR_URI = AttributeKey.valueOf("uri");
    /**
     * Represents the {@code ATTR_REQUEST_URI} attribute, which stores the full request URI of the client
     */
    public static final AttributeKey<String> ATTR_REQUEST_URI = AttributeKey.valueOf("requestUri");
    /**
     * Represents the {@code ATTR_FRAME_HANDLER} attribute, which stores the {@link WsFrameHandler} that processes frames for this client
     */
    public static final AttributeKey<WsFrameHandler> ATTR_FRAME_HANDLER = AttributeKey.valueOf("frameHandler");
    /**
     * Represents the {@code ATTR_PINGER} attribute, which stores the {@link WSServerPinger} that checks connectivity
     */
    public static final AttributeKey<WSServerPinger> ATTR_PINGER = AttributeKey.valueOf("pinger");
    /**
     * Represents the {@code ATTR_ALLOW_NON_SSL} attribute, which stores if the client is allowed to bypass SSL requirements
     */
    public static final AttributeKey<String> ATTR_ALLOW_NON_SSL = AttributeKey.valueOf("allowNonSsl");
    /**
     * Represents a {@link Queue} containing all current WS Sessions
     */
    private static final Queue<Channel> WS_SESSIONS = new ConcurrentLinkedQueue<>();

    /**
     * Default Constructor
     */
    WebSocketFrameHandler() {
        super();
    }

    /**
     * Handles incoming WebSocket frames and passes them to the appropriate {@link WsFrameHandler}
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param frame the {@link WebSocketFrame} containing the request frame
     * @throws Exception passes any thrown exceptions up the stack
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        WsFrameHandler h = ctx.channel().attr(ATTR_FRAME_HANDLER).get();

        if (h.getWsAuthHandler().checkAuthorization(ctx, frame)) {
            WSServerPinger p = ctx.channel().attr(ATTR_PINGER).get();
            if (p != null) {
                p.handleFrame(ctx, frame);
            }

            h.handleFrame(ctx, frame);
        }
    }

    /**
     * Captures {@link HandshakeComplete} events and saves the {@link WsFrameHandler} URI to the session
     *
     * If a handler is not available for the requested path, then {@code 404 NOT FOUND} is sent back to the client using JSON:API format
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param evt the event object
     * @throws Exception passes any thrown exceptions up the stack
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HandshakeComplete hc) {
            String ruri = determineWsFrameHandler(hc.requestUri());
            WsFrameHandler h = wsFrameHandlers.get(ruri);

            if (ruri.isBlank() || h == null) {
                JSONStringer jsonObject = new JSONStringer();
                jsonObject.object().key("errors").array().object()
                        .key("status").value("404")
                        .key("title").value("URI Path Not Found")
                        .key("detail").value("The URI path '" + hc.requestUri() + "' does not have a valid handler")
                        .endObject().endArray().endObject();

                com.gmt2001.Console.debug.println("404 WS: " + hc.requestUri());
                WebSocketFrameHandler.sendWsFrame(ctx, null, WebSocketFrameHandler.prepareTextWebSocketResponse(jsonObject.toString()));
                WebSocketFrameHandler.sendWsFrame(ctx, null, WebSocketFrameHandler.prepareCloseWebSocketFrame(WebSocketCloseStatus.POLICY_VIOLATION));
                ctx.close();
            } else {
                com.gmt2001.Console.debug.println("200 WS: " + hc.requestUri() + "   Remote: [" + ctx.channel().remoteAddress().toString() + "]");
                boolean allowNonSsl = hc.requestHeaders().contains(HTTPWSServer.HEADER_X_FORWARDED_HOST) || hc.requestHeaders().contains(HTTPWSServer.HEADER_CF_RAY);

                if (!allowNonSsl) {
                    QueryStringDecoder qsd = new QueryStringDecoder(hc.requestUri());
                    for (String u : WsSslErrorHandler.ALLOWNONSSLPATHS) {
                        if (qsd.path().startsWith(u)) {
                            allowNonSsl = true;
                            break;
                        }
                    }
                }

                WSServerPinger p = h.pinger(ctx);

                ctx.channel().attr(ATTR_URI).set(ruri);
                ctx.channel().attr(ATTR_REQUEST_URI).set(hc.requestUri());
                ctx.channel().attr(ATTR_FRAME_HANDLER).set(h);
                ctx.channel().attr(ATTR_PINGER).set(p);
                ctx.channel().attr(ATTR_ALLOW_NON_SSL).set(allowNonSsl ? "true" : "false");
                h.getWsAuthHandler().checkAuthorizationHeaders(ctx, hc.requestHeaders(), ruri);
                ctx.channel().attr(WsAuthenticationHandler.ATTR_AUTHENTICATED).setIfAbsent(Boolean.FALSE);
                ctx.channel().closeFuture().addListener((ChannelFutureListener) (ChannelFuture f) -> {
                    WS_SESSIONS.remove(f.channel());
                    WsFrameHandler wh = f.channel().attr(ATTR_FRAME_HANDLER).get();
                    WSServerPinger wp = f.channel().attr(ATTR_PINGER).get();
                    if (wp != null) {
                        wp.onClose(f.channel());
                    }
                    wh.onClose(f.channel());
                });
                WS_SESSIONS.add(ctx.channel());
                if (p != null) {
                    p.handshakeComplete(ctx, hc);
                }
                h.handshakeComplete(ctx, hc);
            }
        }
    }

    /**
     * Handles exceptions that are thrown up the stack
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param cause the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        com.gmt2001.Console.debug.printOrLogStackTrace(cause);
        WebSocketFrameHandler.sendWsFrame(ctx, null, WebSocketFrameHandler.prepareCloseWebSocketFrame(WebSocketCloseStatus.INTERNAL_SERVER_ERROR));
        ctx.close();
    }

    /**
     * Determines the best {@link WsFrameHandler} to use for a given URI
     *
     * @param uri the URI to check
     * @return the key of the {@link WsFrameHandler} to use, or {@code ""} if none were found
     */
    static String determineWsFrameHandler(String uri) {
        String bestMatch = "";

        if (uri.contains("..")) {
            return bestMatch;
        }

        for (String k : wsFrameHandlers.keySet()) {
            if (uri.startsWith(k) && k.length() > bestMatch.length()) {
                bestMatch = k;
            }
        }

        return bestMatch;
    }

    /**
     * Creates and prepares a text-type {@link WebSocketFrame} for transmission
     *
     * @param content the content to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareTextWebSocketResponse(String content) {
        return new TextWebSocketFrame(content);
    }

    /**
     * Creates and prepares a text-type {@link WebSocketFrame} for transmission from a {@link JSONObject}
     *
     * @param json the {@link JSONObject} to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareTextWebSocketResponse(JSONObject json) {
        return new TextWebSocketFrame(json.toString());
    }

    /**
     * Creates and prepares a text-type {@link WebSocketFrame} for transmission from a {@link JSONStringer}
     *
     * @param json the {@link JSONStringer} to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareTextWebSocketResponse(JSONStringer json) {
        return new TextWebSocketFrame(json.toString());
    }

    /**
     * Creates and prepares a binary-type {@link WebSocketFrame} for transmission
     *
     * @param content the binary content to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareBinaryWebSocketResponse(byte[] content) {
        return new BinaryWebSocketFrame(Unpooled.copiedBuffer(content));
    }

    /**
     * Creates and prepares a Close {@link WebSocketFrame} for transmission
     *
     * @param status the {@link WebSocketCloseStatus} to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareCloseWebSocketFrame(WebSocketCloseStatus status) {
        return new CloseWebSocketFrame(status);
    }

    /**
     * Creates and prepares a Close {@link WebSocketFrame} for transmission
     *
     * @param status the close status code to send
     * @param reason the reason string to send
     * @return a {@link WebSocketFrame} that is ready to transmit
     */
    public static WebSocketFrame prepareCloseWebSocketFrame(int status, String reason) {
        return new CloseWebSocketFrame(status, reason);
    }

    /**
     * Transmits a {@link WebSocketFrame} back to the client
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param reqframe the {@link WebSocketFrame} containing the request
     * @param resframe the {@link WebSocketFrame} to transmit
     */
    public static void sendWsFrame(ChannelHandlerContext ctx, WebSocketFrame reqframe, WebSocketFrame resframe) {
        sendWsFrame(ctx.channel(), reqframe, resframe);
    }

    /**
     * Transmits a {@link WebSocketFrame} back to the client
     *
     * @param ch the {@link Channel} of the connection
     * @param reqframe the {@link WebSocketFrame} containing the request
     * @param resframe the {@link WebSocketFrame} to transmit
     */
    public static void sendWsFrame(Channel ch, WebSocketFrame reqframe, WebSocketFrame resframe) {
        try {
            ReferenceCountedUtil.releaseAuto(resframe);
            ch.writeAndFlush(resframe).addListener((p) -> {
                ReferenceCountedUtil.releaseObj(resframe);
            });
        } catch (Exception ex) {
            com.gmt2001.Console.err.printStackTrace(ex);
            ReferenceCountedUtil.releaseObj(resframe);
        }
    }

    /**
     * Transmits a {@link WebSocketFrame} to all authenticated clients
     *
     * @param resframe the {@link WebSocketFrame} to transmit
     */
    public static void broadcastWsFrame(WebSocketFrame resframe) {
        WS_SESSIONS.forEach((c) -> {
            if (c.attr(WsAuthenticationHandler.ATTR_AUTHENTICATED).get()) {
                sendWsFrame(c, null, resframe.copy());
            }
        });

        ReferenceCountedUtil.releaseObj(resframe);
    }

    /**
     * Transmits a {@link WebSocketFrame} to all authenticated clients that are connected to a specific URI
     *
     * @param uri the URI to filter clients by for the broadcast
     * @param resframe the {@link WebSocketFrame} to transmit
     */
    public static void broadcastWsFrame(String uri, WebSocketFrame resframe) {
        com.gmt2001.Console.debug.println("Broadcasting frame to Uri [" + uri + "]");
        WS_SESSIONS.forEach((c) -> {
            if (c.attr(WsAuthenticationHandler.ATTR_AUTHENTICATED).get() && c.attr(ATTR_URI).get().equals(uri)) {
                com.gmt2001.Console.debug.println("Broadcast to client [" + c.remoteAddress().toString() + "]");
                sendWsFrame(c, null, resframe.copy());
            } else {
                com.gmt2001.Console.debug.println("Did not broadcast to client [" + c.remoteAddress().toString() + "] Authenticated: " + (c.attr(WsAuthenticationHandler.ATTR_AUTHENTICATED).get() ? "t" : "f") + "   Uri: " + c.attr(ATTR_URI).get() + "   Uri match: " + (c.attr(ATTR_URI).get().equals(uri) ? "t" : "f"));
            }
        });

        ReferenceCountedUtil.releaseObj(resframe);
    }

    /**
     * Closes all WS sessions
     */
    static void closeAllWsSessions() {
        WebSocketFrame resframe = WebSocketFrameHandler.prepareCloseWebSocketFrame(WebSocketCloseStatus.ENDPOINT_UNAVAILABLE);
        WS_SESSIONS.forEach((c) -> {
            sendWsFrame(c, null, resframe.copy());
            c.close();
        });

        ReferenceCountedUtil.releaseObj(resframe);
    }

    /**
     * Returns a queue containing all authenticated WS session channels that match the given URI
     *
     * @param uri The URI to match
     * @return The matching channels
     */
    public static Queue<Channel> getWsSessions(String uri) {
        Queue<Channel> sessions = new ConcurrentLinkedQueue<>();

        WS_SESSIONS.forEach((c) -> {
            if (c.attr(WsAuthenticationHandler.ATTR_AUTHENTICATED).get() && c.attr(ATTR_URI).get().equals(uri)) {
                sessions.add(c);
            }
        });

        return sessions;
    }

    /**
     * Registers a WS URI path to a {@link WsFrameHandler}
     *
     * @param path the URI path to bind the handler to
     * @param handler the {@link WsFrameHandler} that will handle the requests
     * @throws IllegalArgumentException if {@code path} is either already registered, or illegal
     * @see validateUriPath
     */
    public static void registerWsHandler(String path, WsFrameHandler handler) {
        if (HTTPWSServer.validateUriPath(path, true)) {
            if (wsFrameHandlers.containsKey(path)) {
                throw new IllegalArgumentException("The specified path is already registered. Please unregister it first");
            } else {
                wsFrameHandlers.put(path, handler);
            }
        } else {
            throw new IllegalArgumentException("Illegal path. Must not contain .. and must start with /ws");
        }
    }

    /**
     * Deregisters a WS URI path
     *
     * @param path the path to deregister
     */
    public static void deregisterWsHandler(String path) {
        wsFrameHandlers.remove(path);
    }

}
