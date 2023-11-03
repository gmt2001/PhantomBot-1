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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Represents an Authentication Handler for an {@link HttpHandler}
 *
 * @author gmt2001
 */
public interface HttpAuthenticationHandler {

    /**
     * Checks if the given {@link FullHttpRequest} is a valid authentication request, or if the underlying {@link Channel} has already been
     * authenticated
     * <p>
     * When returning {@code false}, this method MUST also send either an {@code 401 Unauthorized}, a {@code Header Location}, or an error page back
     * to the client
     *
<<<<<<< HEAD
     * @param ctx The {@link ChannelHandlerContext} of the session
     * @param req The {@link FullHttpRequest} to check
     * @return {@code true} if authorized
=======
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param req the {@link FullHttpRequest} to check
     * @return otherwise
>>>>>>> bcb562333 (Normalize JavaDoc case)
     */
    boolean checkAuthorization(ChannelHandlerContext ctx, FullHttpRequest req);

    /**
     * Checks if the given {@link FullHttpRequest} is a valid authentication request, or if the underlying {@link Channel} has already been
     * authenticated
     * <p>
     * When returning {@code false}, this method MUST NOT send a response to the client
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param req the {@link FullHttpRequest} to check
     * @return otherwise
     */
    boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req);

    /**
     * Checks if the given {@link HttpHeaders} contain a valid authorization, or if the underlying {@link Channel} has already been
     * authenticated
     * <p>
     * When returning {@code false}, this method MUST NOT send a response to the client
     *
     * @param ctx the {@link ChannelHandlerContext} of the session
     * @param headers the {@link HttpHeaders} to check
     * @return otherwise
     */
    boolean isAuthorized(ChannelHandlerContext ctx, HttpHeaders headers, String requestUri);

    /**
     * Checks if the given username and password is a valid
     * <p>
     * When returning {@code false}, this method MUST NOT send a response to the client
     *
     * @param user The username
     * @param pass The password
     * @return {@code true} if authorized
     */
    boolean isAuthorized(String user, String pass);
}
