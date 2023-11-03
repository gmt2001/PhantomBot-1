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
import io.netty.util.AttributeKey;
import tv.phantombot.panel.PanelUser.PanelUser;

/**
 * An extension of an authentication handler which can potentially use a {@link PanelUser} for authorization
 */
public interface PanelUserAuthenticationHandler {

    /**
     * Represents the {@code ATTR_AUTH_USER} attribute, which stores the authenticated user
     */
    AttributeKey<PanelUser> ATTR_AUTH_USER = AttributeKey.valueOf("authUser");

    /**
     * Returns the authenticated {@link PanelUser}
     *
     * @param ctx the context
     * @return {@code null} if not authenticated with a PanelUser
     */
    public PanelUser getUser(ChannelHandlerContext ctx);
}
