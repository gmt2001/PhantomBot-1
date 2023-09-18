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
package tv.phantombot.event.webserver;

import org.json.JSONException;
import org.json.JSONObject;

import com.gmt2001.httpwsserver.HttpServerPageHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * A message received from the bots web server via HTTP
 *
 * @author gmt2001
 */
public final class WebServerHTTPMessageEvent extends WebServerMessageEvent {
    /**
     * Construct a JSONObject from another JSONObject
     *
     * @param channelHandlerContext the channel handler context to send replies to
     * @param source a string beginning with {@code &#123;} (left brace) and ending with {@code &#125;} (right brace)
     * @throws JSONException if there is a syntax error in the source string, a duplicated key, there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebServerHTTPMessageEvent(ChannelHandlerContext channelHandlerContext, String source) {
        super(channelHandlerContext, source);
    }

    /**
     * Construct a JSONObject from another JSONObject
     *
     * @param channelHandlerContext the channel handler context to send replies to
     * @param jo a JSONObject
     * @throws JSONException if there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebServerHTTPMessageEvent(ChannelHandlerContext channelHandlerContext, JSONObject jo) {
        super(channelHandlerContext, jo);
    }

    @Override
    protected void doSendResponse(HttpResponseStatus status, String json) {
        if (!this.channelHandlerContext.refersTo(null) && this.channelHandlerContext.get().channel().isActive()) {
            HttpServerPageHandler.sendHttpResponse(this.channelHandlerContext.get(), null,
            HttpServerPageHandler.prepareHttpResponse(status, json));
        }
    }
}
