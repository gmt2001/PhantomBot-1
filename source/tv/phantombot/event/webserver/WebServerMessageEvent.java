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

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.gmt2001.httpwsserver.auth.WsSharedRWTokenAuthenticationHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import tv.phantombot.event.Event;
import tv.phantombot.panel.PanelUser.PanelUser;

/**
 * A message received from the bots web server
 *
 * @author gmt2001
 */
public abstract sealed class WebServerMessageEvent extends JSONObject implements Event
    permits WebServerHTTPMessageEvent, WebServerWebSocketMessageEvent {
    /**
     * Weak reference to the channel handler context which sent the message
     */
    protected final WeakReference<ChannelHandlerContext> channelHandlerContext;
    /**
     * The request id
     */
    protected final String id;
    /**
     * The request target
     */
    protected final String target;
    /**
     * The message type
     */
    protected final String type;

    /**
     * Construct a JSONObject from a source JSON text string
     *
     * @param channelHandlerContext the channel handler context to send replies to
     * @param source a string beginning with {@code &#123;} (left brace) and ending with {@code &#125;} (right brace)
     * @throws JSONException if there is a syntax error in the source string, a duplicated key, there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebServerMessageEvent(ChannelHandlerContext channelHandlerContext, String source) {
        super(source);
        this.channelHandlerContext = new WeakReference<ChannelHandlerContext>(channelHandlerContext);
        this.id = this.getString("id");
        this.target = this.getString("target");
        this.type = this.getString("type");
    }

    /**
     * Construct a JSONObject from another JSONObject
     *
     * @param channelHandlerContext the channel handler context to send replies to
     * @param jo a JSONObject
     * @throws JSONException if there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebServerMessageEvent(ChannelHandlerContext channelHandlerContext, JSONObject jo) {
        super(jo, jo.keySet().toArray(new String[0]));
        this.channelHandlerContext = new WeakReference<ChannelHandlerContext>(channelHandlerContext);
        this.id = this.getString("id");
        this.target = this.getString("target");
        this.type = this.getString("type");
    }

    /**
     * The request id
     *
     * @return the id
     */
    public String id() {
        return this.id;
    }

    /**
     * The request target
     *
     * @return the target
     */
    public String target() {
        return this.target;
    }

    /**
     * The message type
     *
     * @return the type
     */
    public String type() {
        return this.type;
    }

    /**
     * The authenticated user who send the command
     *
     * @return the user; {@code null} if no user is authenticated or the connection has been closed
     */
    public PanelUser user() {
        if (!this.channelHandlerContext.refersTo(null) && this.channelHandlerContext.get().channel().isActive()) {
            return this.channelHandlerContext.get().channel().attr(WsSharedRWTokenAuthenticationHandler.ATTR_AUTH_USER).get();
        }

        return null;
    }

    /**
     * Sends a response message back to the client which originally sent this message
     * <p>
     * The {@link JSONStringer} supplied in the {@code replyBuilder} lambda is already initialized in the middle of a JSON object,
     * therefore, the starting point for implementation should be to call {@link JSONStringer#key(String)} followed by a
     * method which attaches a value to the key
     * <p>
     * The following keys are reserved:
     * <ul>
     * <li>id - value of {@link #id()}</li>
     * <li>target - value of {@link #target()}</li>
     * <li>code - value of {@link HttpResponseStatus#code()}</li>
     * <li>status - value of {@link HttpResponseStatus#reasonPhrase()}</li>
     * <li>type - value of {@code type}</li>
     * </ul>
     * <p>
     * Example: {@code msg.sendResponse("myResponseType" HttpResponseStatus.OK, jsr -> jsr.key("success").value(true));}
     *
     * @param type the message type
     * @param status the response status
     * @param replyBuilder a lambda which will append response data to the response {@link JSONStringer}; {@code null} if not appending any data
     * @throws JSONException if any method in {@link JSONStringer} is called in an illegal order, or if the JSONStringer fails to construct the stringified JSON
     */
    public void sendResponse(String type, HttpResponseStatus status, Consumer<JSONStringer> replyBuilder) {
        if (!this.channelHandlerContext.refersTo(null) && this.channelHandlerContext.get().channel().isActive()) {
            JSONStringer jsr = new JSONStringer();
            jsr.key("id").value(this.id).key("target").value(this.target)
            .key("code").value(status.code()).key("status").value(status.reasonPhrase())
            .key("type").value(type);

            if (replyBuilder != null) {
                replyBuilder.accept(jsr);
            }

            jsr.endObject();

            String json = jsr.toString();

            if (json == null) {
                throw new JSONException("Failed to construct JSON text (unbalanced append?)");
            }

            this.doSendResponse(status, json);
        }
    }

    /**
     * Actually constructs the netty response message object and transmits it
     *
     * @param status the response status
     * @param json the stringified JSON text to transmit
     */
    protected abstract void doSendResponse(HttpResponseStatus status, String json);

    /**
     * Sends an {@code ack} type response message with no additional response data
     * <p>
     * The response code is {@link HttpResponseStatus#ACCEPTED}
     */
    public void ack() {
        this.sendResponse("ack", HttpResponseStatus.ACCEPTED, null);
    }

    /**
     * Sends a {@code nak} type response message with no additional response data
     * <p>
     * The response code is {@link HttpResponseStatus#CONFLICT}
     */
    public void nak() {
        this.sendResponse("nak", HttpResponseStatus.CONFLICT, null);
    }

    /**
     * Sends a {@code forbidden} type response message with no additional response data
     * <p>
     * The response code is {@link HttpResponseStatus#FORBIDDEN}
     */
    public void forbidden() {
        this.sendResponse("forbidden", HttpResponseStatus.FORBIDDEN, null);
    }
}
