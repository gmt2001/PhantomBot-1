/*
 * Copyright (C) 2016-2024 phantombot.github.io/PhantomBot
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
package com.gmt2001.twitch.eventsub.subscriptions.automod.message;

import java.util.Map;

import com.gmt2001.twitch.eventsub.EventSub;
import com.gmt2001.twitch.eventsub.EventSubInternalNotificationEvent;
import com.gmt2001.twitch.eventsub.EventSubSubscription;

import tv.phantombot.event.EventBus;
import tv.phantombot.event.eventsub.automod.message.EventSubAutomodMessageUpdateEvent;

public final class AutomodMessageUpdate extends AutomodMessage {
    public static final String TYPE = "automod.message.update";
    public static final String VERSION = "1";
    private String moderator_user_login;
    private String moderator_user_name;
    private String status;

    /**
     * Only used by EventSub for handler registration
     */
    public AutomodMessageUpdate() {
        super();
        this.subscribe();
    }

    /**
     * Used by {@link onEventSubInternalNotificationEvent} to construct an object from an incoming notification
     *
     * @param e The event
     */
    public AutomodMessageUpdate(EventSubInternalNotificationEvent e) {
        super(e);
        this.moderator_user_login = e.event().getString("moderator_user_login");
        this.moderator_user_name = e.event().getString("moderator_user_name");
        this.status = e.event().getString("status");
    }

    /**
     * Constructor
     *
     * @param broadcaster_user_id The user id of the broadcaster
     */
    public AutomodMessageUpdate(String broadcaster_user_id) {
        this(broadcaster_user_id, EventSub.moderatorUserId());
    }

    /**
     * Constructor
     *
     * @param broadcaster_user_id The user id of the broadcaster
     * @param moderator_user_id The ID of the moderator of the channel you want to get notifications for. If you have authorization from the broadcaster rather than a moderator, specify the broadcaster's user ID here
     */
    public AutomodMessageUpdate(String broadcaster_user_id, String moderator_user_id) {
        super(broadcaster_user_id, moderator_user_id);
    }

    @Override
    protected EventSubSubscription proposeSubscription() {
        return this.proposeSubscriptionInternal(AutomodMessageUpdate.TYPE, AutomodMessageUpdate.VERSION,
            Map.of("broadcaster_user_id", this.broadcasterUserId(), "moderator_user_id", this.moderatorUserId()));
    }

    @Override
    protected void onEventSubInternalNotificationEvent(EventSubInternalNotificationEvent e) {
        try {
            if (e.subscription().type().equals(AutomodMessageUpdate.TYPE)) {
                EventSub.debug(AutomodMessageUpdate.TYPE);
                EventBus.instance().postAsync(new EventSubAutomodMessageUpdateEvent(new AutomodMessageUpdate(e)));
            }
        } catch (Exception ex) {
            com.gmt2001.Console.err.printStackTrace(ex);
        }
    }

    @Override
    protected boolean isMatch(EventSubSubscription subscription) {
        return subscription.type().equals(AutomodMessageUpdate.TYPE)
            && subscription.condition().get("broadcaster_user_id").equals(this.broadcasterUserId())
            && subscription.condition().get("moderator_user_id").equals(this.moderatorUserId());
    }

    /**
     * The moderator's user login.
     * 
     * @return
     */
    public String moderatorUserLogin() {
        return this.moderator_user_login;
    }

    /**
     * The moderator's user display name.
     * 
     * @return
     */
    public String moderatorUserName() {
        return this.moderator_user_name;
    }

    /**
     * The new status of the held message, such as approved, denied, or expired.
     * 
     * @return
     */
    public String status() {
        return this.status;
    }
}
