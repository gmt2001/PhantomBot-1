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
package tv.phantombot.twitch.pubsub.processors;

import java.time.ZonedDateTime;
import org.json.JSONObject;

import tv.phantombot.PhantomBot;
import tv.phantombot.cache.FollowersCache;
import tv.phantombot.event.EventBus;
import tv.phantombot.event.pubsub.following.PubSubFollowEvent;

/**
 * A processor for follow events from PubSub
 *
 * @author gmt2001
 * @deprecated This is now handeled by EventSub
 */
@Deprecated(since = "3.8.0.0", forRemoval = true)
public class PubSubFollowingProcessor extends AbstractPubSubProcessor {

    private final int channelId;
    private final boolean isCaster;

    public PubSubFollowingProcessor() {
        this(PhantomBot.instance().getPubSub().channelId());
    }

    public PubSubFollowingProcessor(int channelId) {
        super("following." + channelId);
        this.channelId = channelId;
        this.isCaster = this.channelId == PhantomBot.instance().getPubSub().channelId();
    }

    @Override
    protected void onOpen() {
        super.onOpen();
        com.gmt2001.Console.out.println("Requesting Twitch Follow Data Feed for " + this.channelId);
    }

    @Override
    protected void onSubscribeSuccess() {
        com.gmt2001.Console.out.println("Connected to Twitch Follow Data Feed for " + this.channelId);
    }

    @Override
    protected void onSubscribeFailure(String error) {
        com.gmt2001.Console.out.println("PubSub Rejected Twitch Follow Data Feed for " + this.channelId + " with Error: " + error);
    }

    @Override
    protected void onEvent(JSONObject body) {
        if (this.isCaster) {
            FollowersCache.instance().addFollow(body.getString("username"), ZonedDateTime.now());
        }
        EventBus.instance().postAsync(new PubSubFollowEvent(body.getString("username"), body.getString("user_id"), body.getString("display_name")));
    }
}
