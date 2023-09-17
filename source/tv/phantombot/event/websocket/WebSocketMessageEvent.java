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
package tv.phantombot.event.websocket;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.gmt2001.httpwsserver.WebSocketFrameHandler;
import com.gmt2001.httpwsserver.auth.WsSharedRWTokenAuthenticationHandler;

import io.netty.channel.Channel;
import tv.phantombot.event.Event;
import tv.phantombot.panel.PanelUser.PanelUser;

/**
 * A message received from a Web Socket on the bots web server
 *
 * @author gmt2001
 */
public final class WebSocketMessageEvent extends JSONObject implements Event {
    /**
     * Weak reference to the channel which sent the message
     */
    private final WeakReference<Channel> channel;
    /**
     * The request id
     */
    private final String id;
    /**
     * The request target
     */
    private final String target;
    /**
     * The message type
     */
    private final String type;

    /**
     * Construct a JSONObject from a source JSON text string
     *
     * @param channel the channel to send replies to
     * @param source a string beginning with {@code &#123;} (left brace) and ending with {@code &#125;} (right brace)
     * @throws JSONException if there is a syntax error in the source string, a duplicated key, there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebSocketMessageEvent(Channel channel, String source) {
        super(source);
        this.channel = new WeakReference<Channel>(channel);
        this.id = this.getString("id");
        this.target = this.getString("target");
        this.type = this.getString("type");
    }

    /**
     * Construct a JSONObject from another JSONObject
     *
     * @param channel the channel to send replies to
     * @param jo a JSONObject
     * @throws JSONException if there is no string value for any of the keys {@code id}, {@code target}, or {@code type}
     */
    public WebSocketMessageEvent(Channel channel, JSONObject jo) {
        super(jo, jo.keySet().toArray(new String[0]));
        this.channel = new WeakReference<Channel>(channel);
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
        if (!this.channel.refersTo(null) && this.channel.get().isActive()) {
            return this.channel.get().attr(WsSharedRWTokenAuthenticationHandler.ATTR_AUTH_USER).get();
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
     * Example: {@code msg.sendResponse("myResponseType", jsr -> jsr.key("success").value(true));}
     *
     * @param type the message type
     * @param replyBuilder a lambda which will append response data to the response {@link JSONStringer}; {@code null} if not appending any data
     * @throws JSONException if any method in {@link JSONStringer} is called in an illegal order, or if the JSONStringer fails to construct the stringified JSON
     */
    public void sendResponse(String type, Consumer<JSONStringer> replyBuilder) {
        if (!this.channel.refersTo(null) && this.channel.get().isActive()) {
            JSONStringer jsr = new JSONStringer();
            jsr.key("id").value(this.id).key("target").value(this.target)
            .key("type").value(type);

            if (replyBuilder != null) {
                replyBuilder.accept(jsr);
            }

            jsr.endObject();

            String json = jsr.toString();

            if (json == null) {
                throw new JSONException("Failed to construct JSON text (unbalanced append?)");
            }

            WebSocketFrameHandler.sendWsFrame(this.channel.get(), null,
            WebSocketFrameHandler.prepareTextWebSocketResponse(json));
        }
    }

    /**
     * Sends an {@code ack} type response message with no additional response data
     */
    public void ack() {
        this.sendResponse("ack", null);
    }

    /**
     * Sends a {@code nak} type response message with no additional response data
     */
    public void nak() {
        this.sendResponse("nak", null);
    }
}
