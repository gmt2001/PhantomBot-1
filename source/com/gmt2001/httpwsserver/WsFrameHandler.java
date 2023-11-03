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

import com.gmt2001.httpwsserver.auth.WsAuthenticationHandler;
import com.gmt2001.wspinger.WSServerPinger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;

/**
 * Represents a handler for WebSocket frames
 *
 * @author gmt2001
 */
public interface WsFrameHandler {

    /**
     * Registers this {@link WsFrameHandler} with the {@link WebSocketFrameHandler}
     *
     * @return
     */
    WsFrameHandler registerWs();

    /**
     * Gets the {@link WsAuthenticationHandler} assigned to this endpoint
     *
     * @return an {@link WsAuthenticationHandler}
     */
    WsAuthenticationHandler getWsAuthHandler();

    /**
     * Handles the WebSocket frame and sends a response back to the client, if necessary
     * <p>
     * Only gets called if the {@link WsAuthenticationHandler} returned {@code true}
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param frame the {@link WebSocketFrame} to process
     */
    void handleFrame(ChannelHandlerContext ctx, WebSocketFrame frame);

    /**
     * Handles the handshake complete event
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param hc the handshake parameters
     */
    default void handshakeComplete(ChannelHandlerContext ctx, HandshakeComplete hc) {}

    /**
     * Handles the channel closing for any reason
     *
     * @param channel the channel that was closed
     */
    default void onClose(Channel channel) {}

    /**
     * Returns a {@link WSServerPinger} to check for connectivity
     *
     * @param ctx the context
     * @return a pinger; {@code null} if not used
     */
    default WSServerPinger pinger(ChannelHandlerContext ctx) {
        return null;
    }
}
