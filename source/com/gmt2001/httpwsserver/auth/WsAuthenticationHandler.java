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
package com.gmt2001.httpwsserver.auth;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;

/**
 * Represents an Authentication Handler for a {@link WsHandler}
 *
 * @author gmt2001
 */
public interface WsAuthenticationHandler {

    /**
     * Represents the {@code ATTR_AUTHENTICATED} attribute
     */
    AttributeKey<Boolean> ATTR_AUTHENTICATED = AttributeKey.valueOf("authenticated");

    /**
     * Represents the {@code attrSentAuthReply} attribute
     */
    public static final AttributeKey<Boolean> ATTR_SENT_AUTH_REPLY = AttributeKey.valueOf("sentAuthReply");

    /**
     * Checks if the given {@link WebSocketFrame} is a valid authentication frame, or if the underlying {@link Channel} has already been authenticated
     * <p>
     * When returning {@code false}, this method MUST also send an {@code Authentication Required} frame back to the client
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param frame the {@link WebSocketFrame} to check
     * @return {@code true} if authorized
     */
    boolean checkAuthorization(ChannelHandlerContext ctx, WebSocketFrame frame);

    /**
     * Checks if the given {@link HttpHeaders} contains a valid authorization, or if the underlying {@link Channel} has already been authenticated
     * <p>
     * When returning {@code false}, this method MUST NOT send any frames back to the client
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param headers the {@link HttpHeaders} to check
     * @param requestUri the request URI
     * @return {@code true} if authorized
     */
    boolean checkAuthorizationHeaders(ChannelHandlerContext ctx, HttpHeaders headers, String requestUri);
}
