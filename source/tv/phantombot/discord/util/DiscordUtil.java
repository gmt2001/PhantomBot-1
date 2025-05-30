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
package tv.phantombot.discord.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gmt2001.PathValidator;
import com.gmt2001.ratelimiters.ExponentialBackoff;
import com.gmt2001.util.concurrent.ExecutorService;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.Channel.Type;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.NewsChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.StoreChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.GuildMemberEditSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.core.util.OrderUtil;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.json.response.ErrorResponse;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tv.phantombot.PhantomBot;
import tv.phantombot.RepoVersion;
import tv.phantombot.discord.DiscordAPI;

/**
 * Has all of the methods to work with Discord4J.
 *
 * @author IllusionaryOne
 * @author ScaniaTV
 */
public class DiscordUtil {

    private final ExponentialBackoff sendBackoff = new ExponentialBackoff(4000L, 60000L, 300000L);

    /**
     * Method that removes the # in the channel name.
     *
     * @param channelName
     * @return
     */
    public String sanitizeChannelName(String channelName) {
        // We have to make sure that it's at the start.
        if (channelName.charAt(0) == '#') {
            return channelName.substring(1);
        } else {
            return channelName;
        }
    }

    public boolean isValidFilePath(String fileLocation) {
        return PathValidator.isValidPathShared(fileLocation);
    }

    private void validateParams(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel was null");
        }
    }

    private void validateParams(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user was null");
        }
    }

    private void validateParams(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role was null");
        }
    }

    private void validateParams(Role... roles) {
        if (roles == null) {
            throw new IllegalArgumentException("roles was null");
        }
    }

    private void validateParams(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message was null, empty, or whitespace");
        }
    }

    private void validateParams(Channel channel, String message) {
        this.validateParams(channel);
        this.validateParams(message);
    }

    private void validateParams(User user, String message) {
        this.validateParams(user);
        this.validateParams(message);
    }

    private void validateParams(User user, Role role) {
        this.validateParams(user);
        this.validateParams(role);
    }

    private void validateParams(User user, Role... roles) {
        this.validateParams(user);
        this.validateParams(roles);
    }

    public Message sendMessage(MessageChannel channel, String message) {
        return this.sendMessageAsync(channel, message).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendMessageAsync(MessageChannel channel, String message) {
        return this.sendMessageAsync(channel, message, false);
    }

    /**
     * Method to send a message to a channel.
     *
     * @param channel
     * @param message
     * @param iteration
     * @return
     */
    private Mono<Message> sendMessageAsync(MessageChannel channel, String message, boolean isRetry) {
        return this.sendMessageAsync(channel, message, isRetry, null);
    }

    private Mono<Message> sendMessageAsync(MessageChannel channel, String message, boolean isRetry, Throwable ex) {
        this.validateParams(channel, message);

        if (RepoVersion.isStressTest()) {
            return Mono.empty();
        }

        if (isRetry && this.sendBackoff.IsAtMaxInterval()) {
            com.gmt2001.Console.debug.println("Failed");
            if (ex != null) {
                com.gmt2001.Console.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            }

            throw new IllegalStateException("connection failing", ex);
        }

        if (isRetry) {
            com.gmt2001.Console.err.println("Request failed, trying again in " + (this.sendBackoff.GetNextInterval() / 1000) + " seconds...");
            this.sendBackoff.Backoff();
        }

        if (channel.getType() == Channel.Type.DM || channel.getType() == Channel.Type.GROUP_DM) {
            com.gmt2001.Console.debug.println("Type is DM");
            this.sendPrivateMessage((PrivateChannel) channel, message);
            return null;
        }

        com.gmt2001.Console.debug.println("Type is Guild");
        return channel.createMessage(message).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).doOnSuccess(m -> {
            if (isRetry) {
                this.sendBackoff.Reset();
            }
            com.gmt2001.Console.out.println("[DISCORD] [#" + DiscordUtil.channelName(channel) + "] [CHAT] " + message);
        }).doOnError(e -> this.sendMessageAsync(channel, message, true, e));
    }

    /**
     * Method to send a message to a channel.
     *
     * @param channelName
     * @param message
     * @return
     */
    public Message sendMessage(String channelName, String message) {
        return this.sendMessageAsync(channelName, message).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendMessageAsync(String channelName, String message) {
        return this.getChannelAsync(channelName, c -> {
            return c.getType() != Type.GUILD_CATEGORY;
        }).flatMap(channel -> this.sendMessageAsync(channel, message));
    }

    /**
     * Method to send private messages to a user.
     *
     * @param user
     * @param message
     */
    public void sendPrivateMessage(User user, String message) {
        this.validateParams(user, message);
        user.getPrivateChannel().doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).doOnSuccess(channel -> this.sendPrivateMessage(channel, message)).subscribe();
    }

    /**
     * Method to send private messages to a user.
     *
     * @param userName
     * @param message
     */
    public void sendPrivateMessage(String userName, String message) {
        this.getUserAsync(userName).doOnSuccess(user -> this.sendPrivateMessage(user, message)).subscribe();
    }

    public void sendPrivateMessage(PrivateChannel channel, String message) {
        this.sendPrivateMessage(channel, message, false);
    }

    /**
     * Method to send private messages to a user.
     *
     * @param channel
     * @param message
     * @param iteration
     */
    private void sendPrivateMessage(PrivateChannel channel, String message, boolean isRetry) {
        this.sendPrivateMessage(channel, message, isRetry, null);
    }

    private void sendPrivateMessage(PrivateChannel channel, String message, boolean isRetry, Throwable ex) {
        this.validateParams(channel, message);

        if (RepoVersion.isStressTest()) {
            return;
        }

        if (isRetry && this.sendBackoff.IsAtMaxInterval()) {
            if (ex != null) {
                com.gmt2001.Console.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            }

            throw new IllegalStateException("connection failing", ex);
        }

        if (isRetry) {
            com.gmt2001.Console.err.println("Request failed, trying again in " + (this.sendBackoff.GetNextInterval() / 1000) + " seconds...");
            this.sendBackoff.Backoff();
        }

        User user = channel.getRecipients().stream().findFirst().orElse(null);
        String uname;

        if (user != null) {
            uname = user.getUsername().toLowerCase();
        } else {
            uname = "";
        }

        channel.createMessage(message).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).doOnSuccess(m -> {
            if (isRetry) {
                this.sendBackoff.Reset();
            }
            com.gmt2001.Console.out.println("[DISCORD] [@" + uname +"] [DM] " + message);
        }).doOnError(e -> this.sendPrivateMessage(channel, message, true, e)).subscribe();
    }

    public Message sendMessageEmbed(GuildMessageChannel channel, EmbedCreateSpec embed) {
        return this.sendMessageEmbedAsync(channel, embed).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendMessageEmbedAsync(GuildMessageChannel channel, EmbedCreateSpec embed) {
        return this.sendMessageEmbedAsync(channel, embed, false);
    }

    /**
     * Method to send embed messages.
     *
     * @param channel
     * @param embed
     * @param iteration
     * @return
     */
    private Mono<Message> sendMessageEmbedAsync(GuildMessageChannel channel, EmbedCreateSpec embed, boolean isRetry) {
        return this.sendMessageEmbedAsync(channel, embed, isRetry, null);
    }

    private Mono<Message> sendMessageEmbedAsync(GuildMessageChannel channel, EmbedCreateSpec embed, boolean isRetry, Throwable ex) {
        this.validateParams(channel);

        if (RepoVersion.isStressTest()) {
            return Mono.empty();
        }


        if (isRetry && this.sendBackoff.IsAtMaxInterval()) {
            if (ex != null) {
                com.gmt2001.Console.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            }

            throw new IllegalStateException("connection failing", ex);
        }

        if (isRetry) {
            com.gmt2001.Console.err.println("Request failed, trying again in " + (this.sendBackoff.GetNextInterval() / 1000) + " seconds...");
            this.sendBackoff.Backoff();
        }

        return channel.createMessage(MessageCreateSpec.create().withEmbeds(embed)
        ).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).doOnSuccess(m -> {
            if (isRetry) {
                this.sendBackoff.Reset();
            }
            com.gmt2001.Console.out.println("[DISCORD] [#" + DiscordUtil.channelName(channel) + "] [EMBED] " + m.getEmbeds().get(0).getDescription().orElse(m.getEmbeds().get(0).getTitle().orElse("")));
        }).doOnError(e -> this.sendMessageEmbedAsync(channel, embed, true, e));
    }

    /**
     * Method to send embed messages.
     *
     * @param channelName
     * @param embed
     * @return
     */
    public Message sendMessageEmbed(String channelName, EmbedCreateSpec embed) {
        return this.sendMessageEmbedAsync(channelName, embed).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendMessageEmbedAsync(String channelName, EmbedCreateSpec embed) {
        return this.getChannelAsync(channelName, c -> {
            return c.getType() != Type.GUILD_CATEGORY;
        }).flatMap(channel -> this.sendMessageEmbedAsync(channel, embed));
    }

    /**
     * Method to send embed messages.
     *
     * @param channel
     * @param message
     * @param color
     * @return
     */
    public Message sendMessageEmbed(GuildMessageChannel channel, String color, String message) {
        return this.sendMessageEmbedAsync(channel, color, message).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendMessageEmbedAsync(GuildMessageChannel channel, String color, String message) {
        this.validateParams(channel, message);
        EmbedCreateSpec embed = new EmbedBuilder().withColor(this.getColor(color)).withDescription(message).build();
        return this.sendMessageEmbedAsync(channel, embed);
    }

    /**
     * Method to send embed messages.
     *
     * @param channelName
     * @param message
     * @param color
     * @return
     */
    public Message sendMessageEmbed(String channelName, String color, String message) {
        this.validateParams(message);
        EmbedCreateSpec embed = new EmbedBuilder().withColor(this.getColor(color)).withDescription(message).build();
        return this.sendMessageEmbedAsync(channelName, embed).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Message sendFile(GuildMessageChannel channel, String message, String fileLocation) {
        return this.sendFileAsync(channel, message, fileLocation).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendFileAsync(GuildMessageChannel channel, String message, String fileLocation) {
        return this.sendFileAsync(channel, message, new MessageCreateFile(fileLocation, fileLocation), false);
    }

    public Mono<Message> sendFileAsync(GuildMessageChannel channel, String message, MessageCreateFile file) {
        return this.sendFileAsync(channel, message, file, false);
    }

    /**
     * Method to send a file to a channel.
     *
     * @param channel
     * @param message
     * @param fileLocation
     * @param iteration
     * @return
     */
    private Mono<Message> sendFileAsync(GuildMessageChannel channel, String message, MessageCreateFile file, boolean isRetry) {
        return this.sendFileAsync(channel, message, file, isRetry, null);
    }

    private Mono<Message> sendFileAsync(GuildMessageChannel channel, String message, MessageCreateFile file, boolean isRetry, Throwable ex) {
        this.validateParams(channel);

        if (RepoVersion.isStressTest()) {
            return Mono.empty();
        }


        if (isRetry && this.sendBackoff.IsAtMaxInterval()) {
            if (ex != null) {
                com.gmt2001.Console.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            }

            throw new IllegalStateException("connection failing", ex);
        }

        if (isRetry) {
            com.gmt2001.Console.err.println("Request failed, trying again in " + (this.sendBackoff.GetNextInterval() / 1000) + " seconds...");
            this.sendBackoff.Backoff();
        }

        if (!this.isValidFilePath(file.path())) {
            com.gmt2001.Console.err.println("[DISCORD] [#" + DiscordUtil.channelName(channel) + "] [UPLOAD] [" + file.name() + "] Rejecting fileLocation");
            return null;
        } else {
            if (message == null || message.isBlank()) {
                return channel.createMessage(MessageCreateSpec.create().withFiles(file)
                ).doOnError(e -> {
                    com.gmt2001.Console.err.printStackTrace(e);
                }).doOnSuccess(m -> {
                    if (isRetry) {
                        this.sendBackoff.Reset();
                    }
                    com.gmt2001.Console.out.println("[DISCORD] [#" + DiscordUtil.channelName(channel) + "] [UPLOAD] [" + file.name() + "]");
                }).doOnError(e -> this.sendFileAsync(channel, message, file, true, e));
            } else {
                return channel.createMessage(MessageCreateSpec.create().withFiles(file).withContent(message)
                ).doOnError(e -> {
                    com.gmt2001.Console.err.printStackTrace(e);
                }).doOnSuccess(m -> {
                    if (isRetry) {
                        this.sendBackoff.Reset();
                    }
                    com.gmt2001.Console.out.println("[DISCORD] [#" + DiscordUtil.channelName(channel) + "] [UPLOAD] [" + file.name() + "] " + message);
                }).doOnError(e -> this.sendFileAsync(channel, message, file, true));
            }
        }
    }

    /**
     * Method to send a file to a channel.
     *
     * @param channelName
     * @param message
     * @param fileLocation
     * @return
     */
    public Message sendFile(String channelName, String message, String fileLocation) {
        return this.sendFileAsync(channelName, message, fileLocation).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Message> sendFileAsync(String channelName, String message, String fileLocation) {
        return this.getChannelAsync(channelName, c -> {
            return c.getType() != Type.GUILD_CATEGORY;
        }).flatMap(channel -> this.sendFileAsync(channel, message, fileLocation));
    }

    /**
     * Method to send a file to a channel.
     *
     * @param channel
     * @param fileLocation
     * @return
     */
    public Message sendFile(GuildMessageChannel channel, String fileLocation) {
        return this.sendFile(channel, "", fileLocation);
    }

    /**
     * Method to send a file to a channel.
     *
     * @param channelName
     * @param fileLocation
     * @return
     */
    public Message sendFile(String channelName, String fileLocation) {
        return this.sendFileAsync(channelName, "", fileLocation).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method that adds a reaction to a message.
     *
     * @param message The message object
     * @param emoji The reaction object
     */
    public void addReaction(Message message, ReactionEmoji emoji) {
        if (message != null && emoji != null) {
            message.addReaction(emoji).doOnError(e -> {
                com.gmt2001.Console.err.printStackTrace(e);
            }).subscribe();
        } else {
            throw new IllegalArgumentException("message or emoji object was null");
        }
    }

    /**
     * Method that adds multiple reactions to a message.
     *
     * @param message The message object
     * @param emojis The reaction objects
     */
    public void addReactions(Message message, ReactionEmoji... emojis) {
        for (ReactionEmoji emoji : emojis) {
            this.addReaction(message, emoji);
        }
    }

    /**
     * Method that adds a reaction to a message.
     *
     * @param message The message object
     * @param emoji The emoji unicode
     */
    public void addReaction(Message message, String emoji) {
        DiscordAPI.getGuild().getEmojis().collectList().doOnSuccess(gel -> {
            ReactionEmoji re = null;

            if (gel != null) {
                for (GuildEmoji ge : gel) {
                    if (ge.getName().equalsIgnoreCase(emoji)) {
                        re = ReactionEmoji.custom(ge);
                    }
                }
            }

            if (re == null) {
                re = ReactionEmoji.unicode(emoji);
            }

            this.addReaction(message, re);
        }).subscribe();
    }

    /**
     * Method that adds a reaction to a message.
     *
     * @param message The message object
     * @param emojis The emoji unicodes
     */
    public void addReactions(Message message, String... emojis) {
        DiscordAPI.getGuild().getEmojis().collectList().doOnSuccess(gel -> {
            ReactionEmoji re;
            for (String emoji : emojis) {
                re = null;

                if (gel != null) {
                    for (GuildEmoji ge : gel) {
                        if (ge.getName().equalsIgnoreCase(emoji)) {
                            re = ReactionEmoji.custom(ge);
                        }
                    }
                }

                if (re == null) {
                    re = ReactionEmoji.unicode(emoji);
                }

                this.addReaction(message, re);
            }
        }).subscribe();
    }

    /**
     * *
     * Indicates if the channel is a type that is allowed to publish crossposts
     *
     * @param channelName The channel to check
     * @return
     */
    public boolean canChannelPublish(String channelName) {
        return this.canChannelPublish(this.getChannel(channelName));
    }

    /**
     * *
     * Indicates if the channel is a type that is allowed to publish crossposts
     *
     * @param channel The channel to check
     * @return
     */
    public boolean canChannelPublish(GuildMessageChannel channel) {
        return channel.getType() == Channel.Type.GUILD_NEWS;
    }

    /**
     * Method to return a channel object by its name.
     *
     * @param channelName - The name of the channel.
     * @return
     */
    public GuildMessageChannel getChannel(String channelName) {
        return this.getChannelAsync(channelName).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }
    public Mono<GuildMessageChannel> getChannelAsync(String channelName) {
        return this.getChannelAsync(channelName, c -> { return true; });
    }

    public Mono<GuildMessageChannel> getChannelAsync(String channelName, Predicate<? super GuildChannel> filter) {
        String schannelName = sanitizeChannelName(channelName);
        try {
            return DiscordAPI.getGuild().getChannels().filter(channel -> channel.getType() != Channel.Type.UNKNOWN).filter(channel -> DiscordUtil.channelName(channel).equalsIgnoreCase(schannelName)
                    || DiscordUtil.channelIdAsString(channel).equals(schannelName))
                    .filter(filter).take(1).single().map(c -> (GuildMessageChannel) c);
        } catch (NoSuchElementException ex) {
            com.gmt2001.Console.err.println("Unable to find channelName [" + channelName + "]");
            throw ex;
        }
    }

    public Map<String, Map<String, String>> getAllChannelInfo() {
        HashMap<String, Map<String, String>> data = new HashMap<>();
        try {
            this.getAllChannelInfoAsync(data).onErrorReturn(null).blockLast(Duration.ofSeconds(5L));
        } catch (NullPointerException ex) {
        }
        return data;
    }

    public Flux<GuildChannel> getAllChannelInfoAsync(Map<String, Map<String, String>> data) {
        return DiscordAPI.getGuild().getChannels().filter(channel -> channel.getType() != Channel.Type.UNKNOWN).doOnNext(channel -> {
            if (null != channel.getType()) {
                switch (channel.getType()) {
                    case GUILD_CATEGORY:
                        Category rc = (Category) channel;
                        data.putIfAbsent(rc.getId().asString(), new HashMap<>());
                        data.get(rc.getId().asString()).putIfAbsent("name", rc.getName());
                        break;
                    case GUILD_NEWS:
                        NewsChannel rn = (NewsChannel) channel;
                        if (!data.containsKey(rn.getCategoryId().map(c -> c.asString()).orElse(""))) {
                            data.putIfAbsent(rn.getCategoryId().map(c -> c.asString()).orElse(""), new HashMap<>());
                        }

                        data.get(rn.getCategoryId().map(c -> c.asString()).orElse("")).putIfAbsent(rn.getId().asString(), rn.getType().name() + ":" + rn.getName());
                        break;
                    case GUILD_STAGE_VOICE:
                        VoiceChannel rsv = (VoiceChannel) channel;
                        if (!data.containsKey(rsv.getCategoryId().map(c -> c.asString()).orElse(""))) {
                            data.putIfAbsent(rsv.getCategoryId().map(c -> c.asString()).orElse(""), new HashMap<>());
                        }

                        data.get(rsv.getCategoryId().map(c -> c.asString()).orElse("")).putIfAbsent(rsv.getId().asString(), rsv.getType().name() + ":" + rsv.getName());
                        break;
                    case GUILD_STORE:
                        StoreChannel rs = (StoreChannel) channel;
                        if (!data.containsKey(rs.getCategoryId().map(c -> c.asString()).orElse(""))) {
                            data.putIfAbsent(rs.getCategoryId().map(c -> c.asString()).orElse(""), new HashMap<>());
                        }

                        data.get(rs.getCategoryId().map(c -> c.asString()).orElse("")).putIfAbsent(rs.getId().asString(), rs.getType().name() + ":" + rs.getName());
                        break;
                    case GUILD_TEXT:
                        TextChannel rt = (TextChannel) channel;
                        if (!data.containsKey(rt.getCategoryId().map(c -> c.asString()).orElse(""))) {
                            data.putIfAbsent(rt.getCategoryId().map(c -> c.asString()).orElse(""), new HashMap<>());
                        }

                        data.get(rt.getCategoryId().map(c -> c.asString()).orElse("")).putIfAbsent(rt.getId().asString(), rt.getType().name() + ":" + rt.getName());
                        break;
                    case GUILD_VOICE:
                        VoiceChannel rv = (VoiceChannel) channel;
                        if (!data.containsKey(rv.getCategoryId().map(c -> c.asString()).orElse(""))) {
                            data.putIfAbsent(rv.getCategoryId().map(c -> c.asString()).orElse(""), new HashMap<>());
                        }

                        data.get(rv.getCategoryId().map(c -> c.asString()).orElse("")).putIfAbsent(rv.getId().asString(), rv.getType().name() + ":" + rv.getName());
                        break;
                    default:
                        break;
                }
            }
        }).doFinally((s) -> {
            data.forEach((category, channels) -> {
                data.get(category).putIfAbsent("name", category);
            });
        });
    }

    /**
     * Method to return a channel object by its ID.
     *
     * @param channelId - The string ID of the channel
     * @return
     */
    public GuildMessageChannel getChannelByID(String channelId) {
        return this.getChannelByIDAsync(channelId).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<GuildMessageChannel> getChannelByIDAsync(String channelId) {
        try {
            return DiscordAPI.getGuild().getChannels().filter(channel -> channel.getType() != Channel.Type.UNKNOWN).filter(channel -> DiscordUtil.channelIdAsString(channel).equals(channelId)).take(1).single().map(c -> (GuildMessageChannel) c);
        } catch (NoSuchElementException ex) {
            com.gmt2001.Console.err.println("Unable to find channelId [" + channelId + "]");
            throw ex;
        }
    }

    public User getUser(String userName) {
        return this.getUserAsync(userName).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method to return a user object by its name.
     *
     * @param userName - The user's name.
     * @return
     */
    public Mono<User> getUserAsync(String userName) {
        Flux<Member> members = DiscordAPI.getGuild().getMembers();

        if (PhantomBot.getEnableDebugging()) {
            com.gmt2001.Console.debug.println(userName);
            com.gmt2001.Console.debug.println(members.count().block());
        }

        Flux<Member> filteredMembers = members.filter(user -> user.getDisplayName().equalsIgnoreCase(userName) || user.getUsername().equalsIgnoreCase(userName) || user.getMention().equalsIgnoreCase(userName) || user.getNicknameMention().equalsIgnoreCase(userName) || user.getId().asString().equalsIgnoreCase(userName));

        if (PhantomBot.getEnableDebugging()) {
            com.gmt2001.Console.debug.println(filteredMembers.count().block());
        }

        return filteredMembers.take(1)
                                .singleOrEmpty()
                                .switchIfEmpty(Mono.error(new NoSuchElementException("Unable to find userName [" + userName + "]")))
                                .map(m -> (User) m)
                                .doOnError(e -> com.gmt2001.Console.err.printStackTrace(e));
    }

    public User getUserById(long userId) {
        return this.getUserByIdAsync(userId).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method to return a user object by its id.
     *
     * @param userId - The ID of the user.
     * @return
     */
    public Mono<User> getUserByIdAsync(long userId) {
        try {
            return DiscordAPI.getGuild().getMembers().filter(user -> user.getId().asLong() == userId).take(1).single().map(m -> (User) m);
        } catch (NoSuchElementException ex) {
            com.gmt2001.Console.err.println("Unable to find userId [" + userId + "]");
            throw ex;
        }
    }

    /**
     * @deprecated Discriminators have been removed from Discord
     */
    @Deprecated(since = "3.10.0.0", forRemoval = true)
    public User getUserWithDiscriminator(String userName, String discriminator) {
        return this.getUserWithDiscriminatorAsync(userName, discriminator).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method to return a user object by its name and its discriminator.
     *
     * @param userName
     * @param discriminator
     * @return
     * @deprecated Discriminators have been removed from Discord
     */
    @Deprecated(since = "3.10.0.0", forRemoval = true)
    public Mono<User> getUserWithDiscriminatorAsync(String userName, String discriminator) {
        try {
            return DiscordAPI.getGuild().getMembers().filter(user -> user.getDisplayName().equalsIgnoreCase(userName)
                    && user.getDiscriminator().equalsIgnoreCase(discriminator)).take(1).single().map(m -> (User) m);
        } catch (NoSuchElementException ex) {
            com.gmt2001.Console.err.println("Unable to find userNameDiscriminator [" + userName + "#" + discriminator + "]");
            throw ex;
        }
    }

    /**
     * Search for Discord roles by display name or mention and return a Flux
     * <br />
     * Matching is performed case insensitively
     * <br />
     * The Flux is sorted so the roles are returned in permission order,
     * as displayed in the Discord roles list of the Guild settings.
     * The sort starts with the cloest role to @everyone, and then continues up
     *
     * @param roleNames The role names or mentions to find
     * @return a Flux containing matching roles
     */
    public Flux<Role> getRolesAsync(String... roleNames) {
        Flux<Role> roles = DiscordAPI.getGuild().getRoles().transform(OrderUtil::orderRoles);

        if (PhantomBot.getEnableDebugging()) {
            com.gmt2001.Console.debug.println(roles.count().block());
        }

        Flux<Role> filteredRoles = roles.filter(role -> {
            String targetRoleName = role.getName();
            String targetRoleMention = role.getMention();
            for (String roleName: roleNames) {
                if (targetRoleName.equalsIgnoreCase(roleName) || targetRoleMention.equalsIgnoreCase(roleName)) {
                    return true;
                }
            }

            return false;
        });

        if (PhantomBot.getEnableDebugging()) {
            com.gmt2001.Console.debug.println(filteredRoles.count().block());
        }

        return filteredRoles;
    }

    public Role getRole(String roleName) {
        return this.getRoleAsync(roleName).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method to return a role object by its name.
     *
     * @param roleName
     * @return
     */
    public Mono<Role> getRoleAsync(String roleName) {
        return this.getRolesAsync(roleName).take(1)
                            .singleOrEmpty()
                            .switchIfEmpty(Mono.error(new NoSuchElementException("Unable to find roleName [" + roleName + "]")))
                            .doOnError(e -> com.gmt2001.Console.err.printStackTrace(e));
    }

    public Role getRoleByID(String id) {
        return this.getRoleByIDAsync(id).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    /**
     * Method that returns a role by its ID.
     *
     * @param id
     * @return
     */
    public Mono<Role> getRoleByIDAsync(String id) {
        try {
            return DiscordAPI.getGuild().getRoles().filter(role -> role.getId().asString().equalsIgnoreCase(id)).take(1).single();
        } catch (NoSuchElementException ex) {
            com.gmt2001.Console.err.println("Unable to find roleId [" + id + "]");
            throw ex;
        }
    }

    /**
     * Method to get an array of role objects by a string of role names.
     *
     * @param roles
     * @return
     */
    public Role[] getRoleObjects(String... roles) {
        return this.getRoleObjectsAsync(roles).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Role[]> getRoleObjectsAsync(String... roles) {
        return Mono.fromCallable(() -> this.getRolesAsync(roles).toStream().toArray(i -> new Role[i]));
    }

    /**
     * Method to get a list of a user's roles.
     *
     * @param user
     * @return
     */
    public Role[] getUserRoles(User user) {
        this.validateParams(user);
        return this.getUserRolesAsync(user).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Role[]> getUserRolesAsync(User user) {
        this.validateParams(user);
        return user.asMember(DiscordAPI.getGuildId()).flatMap(m -> m.getRoles().collectList().map(roles -> roles.isEmpty() ? new Role[0] : roles.toArray(Role[]::new))).onErrorReturn(new Role[0]);
    }

    /**
     * Method to get a list of a user's roles.
     *
     * @param userId
     * @return
     */
    public Role[] getUserRoles(String userId) {
        return this.getUserRolesAsync(userId).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<Role[]> getUserRolesAsync(String userId) {
        return this.getUserByIdAsync(Long.parseUnsignedLong(userId)).flatMap(user -> this.getUserRolesAsync(user));
    }

    /**
     * Method to edit roles on a user, multiple can be set at once to replace the current ones.
     *
     * @param user
     * @param roles
     */
    public void editUserRoles(User user, Role... roles) {
        this.validateParams(user, roles);

        user.asMember(DiscordAPI.getGuildId()).doOnSuccess(m -> {
            Set<Snowflake> rolesSf = new HashSet<>();

            for (Role role : roles) {
                rolesSf.add(role.getId());
            }

            m.edit(GuildMemberEditSpec.create().withRoles(rolesSf)
            ).doOnError(e -> {
                com.gmt2001.Console.err.println("Unable to edit member roles" + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
                com.gmt2001.Console.err.printStackTrace(e);
            }).subscribe();
        }).doOnError(e -> {
            com.gmt2001.Console.err.println("Unable to convert user to member " + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    /**
     * Method to edit roles on a user, multiple can be set at once to replace the current ones.
     *
     * @param userId
     * @param roles
     */
    public void editUserRoles(String userId, Role... roles) {
        this.getUserByIdAsync(Long.parseUnsignedLong(userId)).subscribe(user -> this.editUserRoles(user, roles));
    }

    /**
     * Method to set a role on a user.
     *
     * @param role
     * @param user
     */
    public void addRole(Role role, User user) {
        this.validateParams(user, role);

        user.asMember(DiscordAPI.getGuildId()).doOnSuccess(m -> {
            m.addRole(role.getId()).doOnError(e -> {
                com.gmt2001.Console.err.println("Unable to add member role" + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
                com.gmt2001.Console.err.printStackTrace(e);
            }).subscribe();
        }).doOnError(e -> {
            com.gmt2001.Console.err.println("Unable to convert user to member " + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    /**
     * Method to set a role on a user.
     *
     * @param roleName
     * @param userName
     */
    public void addRole(String roleName, String userName) {
        com.gmt2001.Console.debug.println(userName + " > " + roleName);
        this.getUserAsync(userName).subscribe(user -> {
            com.gmt2001.Console.debug.println(user);
            this.addRole(roleName, user);
        });
    }

    /**
     * Method to set a role on a user.
     *
     * @param roleName
     * @param user
     */
    public void addRole(String roleName, User user) {
        this.getRoleAsync(roleName).subscribe(role -> {
            com.gmt2001.Console.debug.println(role);
            this.addRole(role, user);
        });
    }

    /**
     * Method to remove a role on a user.
     *
     * @param role
     * @param user
     */
    public void removeRole(Role role, User user) {
        this.validateParams(user, role);

        user.asMember(DiscordAPI.getGuildId()).doOnSuccess(m -> {
            m.removeRole(role.getId()).doOnError(e -> {
                com.gmt2001.Console.err.println("Unable to remove member role" + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
                com.gmt2001.Console.err.printStackTrace(e);
            }).subscribe();
        }).doOnError(e -> {
            com.gmt2001.Console.err.println("Unable to convert user to member " + user.getMention() + " (" + DiscordAPI.getGuild().getName() + ")");
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    /**
     * Method to remove a role on a user.
     *
     * @param roleName
     * @param userName
     */
    public void removeRole(String roleName, String userName) {
        this.getRoleAsync(roleName).subscribe(role ->
            this.getUserAsync(userName).subscribe(user ->
                this.removeRole(role, user))
        );
    }

    /**
     * Method to create a new role.
     *
     * @param roleName
     */
    public void createRole(String roleName) {
        DiscordAPI.getGuild().createRole(RoleCreateSpec.create().withName(roleName)
        ).doOnError(e -> {
            com.gmt2001.Console.err.println("Unable to create role" + roleName + " (" + DiscordAPI.getGuild().getName() + ")");
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    /**
     * Method to delete a role.
     *
     * @param role
     */
    public void deleteRole(Role role) {
        this.validateParams(role);
        role.delete().doOnError(e -> {
            com.gmt2001.Console.err.println("Unable to delete role " + role.getName() + " (" + DiscordAPI.getGuild().getName() + ")");
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    /**
     * Method to delete a role.
     *
     * @param roleName
     */
    public void deleteRole(String roleName) {
        this.getRoleAsync(roleName).subscribe(role -> this.deleteRole(role));
    }

    /**
     * Method that gets a list of guild roles.
     *
     * @return
     */
    public List<Role> getGuildRoles() {
        return this.getGuildRolesAsync().doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<List<Role>> getGuildRolesAsync() {
        return Optional.ofNullable(Optional.ofNullable(DiscordAPI.getGuild()).map(Guild::getRoles).orElseGet(() -> Flux.<Role>empty())).map(Flux<Role>::collectList).orElseGet(() -> {
            return Flux.<Role>empty().collectList();
        });
    }

    /**
     * Method to check if someone is an administrator.
     *
     * @param user
     * @return
     */
    public boolean isAdministrator(User user) {
        this.validateParams(user);
        try {
            return this.isAdministratorAsync(user).block();
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public Mono<Boolean> isAdministratorAsync(User user) {
        this.validateParams(user);

        return user.asMember(DiscordAPI.getGuildId()).flatMap(m -> m.getBasePermissions()).map(ps -> ps != null && ps.contains(Permission.ADMINISTRATOR)).onErrorReturn(false);
    }

    /**
     * Method to check if someone is an administrator.
     *
     * @param userName
     * @return
     */
    public boolean isAdministrator(String userName) {
        try {
            return this.isAdministratorAsync(userName).block();
        } catch (NullPointerException ex) {
            return false;
        }
    }

    public Mono<Boolean> isAdministratorAsync(String userName) {
        return this.getUserAsync(userName).flatMap(user -> this.isAdministratorAsync(user));
    }

    /**
     * Method to bulk delete messages from a channel.
     *
     * @param channel
     * @param amount
     */
    public void bulkDelete(GuildMessageChannel channel, int amount) {
        // Discord4J says that getting messages can block the current thread if they need to be requested from Discord's API.
        // So start this on a new thread to avoid that. Please note that you need to delete at least 2 messages.
        this.validateParams(channel);
        if (amount < 2) {
            throw new IllegalArgumentException("amount was less than 2");
        }

        com.gmt2001.Console.debug.println("Attempting to delete " + amount + " messages from " + DiscordUtil.channelName(channel));
        ExecutorService.submit(() -> {
            Thread.currentThread().setName("tv.phantombot.discord.util.DiscordUtil::bulkDelete");
            channel.getMessagesBefore(channel.getLastMessageId().orElseThrow()).take(amount).collectList().doOnSuccess(msgs -> {
                com.gmt2001.Console.debug.println("Found " + msgs.size() + " messages to delete");
                channel.bulkDelete(Flux.fromIterable(msgs).map(msg -> msg.getId())).doOnNext(s -> com.gmt2001.Console.err.println("Rejected message " + s.asString() + " from delete operation for being too old")).doOnError(e -> com.gmt2001.Console.debug.printStackTrace(e)).doOnComplete(() -> com.gmt2001.Console.debug.println("Bulk delete complete")).subscribe();
            }).subscribe();
        });
    }

    /**
     * Method to bulk delete messages from a channel.
     *
     * @param channelName
     * @param amount
     */
    public void bulkDelete(String channelName, int amount) {
        try {
            this.getChannelAsync(channelName, c -> {
                return c.getType() != Type.GUILD_CATEGORY;
            }).onErrorReturn(null).subscribe(channel -> this.bulkDelete(channel, amount));
        } catch (NullPointerException ex) {
        }
    }

    /**
     * Method to bulk delete messages from a channel.
     *
     * @param channel
     * @param list
     */
    public void bulkDeleteMessages(GuildMessageChannel channel, Message... list) {
        this.validateParams(channel);
        if (list == null || list.length < 2) {
            throw new IllegalArgumentException("list object was null, or amount was less than 2");
        }

        Flux<Snowflake> msgSfs = Flux.empty();

        for (Message msg : list) {
            Flux.concat(msgSfs, Flux.just(msg.getId()));
        }

        channel.bulkDelete(msgSfs);
    }

    /**
     * Method to bulk delete messages from a channel.
     *
     * @param channelName
     * @param messages
     */
    public void bulkDeleteMessages(String channelName, Message... messages) {
        try {
            this.getChannelAsync(channelName, c -> {
                return c.getType() != Type.GUILD_CATEGORY;
            }).onErrorReturn(null).subscribe(channel -> this.bulkDeleteMessages(channel, messages));
        } catch (NullPointerException ex) {
        }
    }

    /**
     * Method to delete a message.
     *
     * @param message
     */
    public void deleteMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message object was null");
        }

        com.gmt2001.Console.debug.println("Deleteing Discord message: " + message.getId().asString());

        message.delete().doOnError(e -> {
            if (e instanceof ClientException ce) {
                ErrorResponse er = ce.getErrorResponse().get();
                if (er != null && er.getFields().containsKey("errorResponse")) {
                    ErrorResponse er2 = (ErrorResponse) er.getFields().get("errorResponse");
                    if (er2 != null && er2.getFields().containsKey("code") && (int) er2.getFields().get("code") == 10008) {
                        com.gmt2001.Console.err.println("Delete message failed (Unknown Message): " + message.getId().asString());
                        com.gmt2001.Console.debug.printStackTrace(e);
                        return;
                    }
                }
            }

            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void setCompeting(String competingIn) {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online(ClientActivity.competing(competingIn))).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void setListening(String listeningTo) {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online(ClientActivity.listening(listeningTo))).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void setWatching(String watching) {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online(ClientActivity.watching(watching))).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void setPlaying(String game) {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online(ClientActivity.playing(game))).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void setStreaming(String streaming, String url) {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online(ClientActivity.streaming(streaming, url))).doOnError(e -> {
            com.gmt2001.Console.err.printStackTrace(e);
        }).subscribe();
    }

    public void resetPresence() {
        DiscordAPI.getGateway().updatePresence(ClientPresence.online()).subscribe();
    }

    /**
     * Method to set the current game.
     *
     * @param game
     */
    public void setGame(String game) {
        this.setPlaying(game);
    }

    /**
     * Method to set the current game and stream.
     *
     * @param game
     * @param url
     */
    public void setStream(String game, String url) {
        this.setStreaming(game, url);
    }

    /**
     * Method to remove the current game or reset the streaming status.
     */
    public void removeGame() {
        this.resetPresence();
    }

    /**
     * Method that gets all server members
     *
     * @return
     */
    public List<User> getUsers() {
        return this.getUsersAsync().doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
    }

    public Mono<List<User>> getUsersAsync() {
        return DiscordAPI.getGuild().getMembers().map(m -> (User) m).collectList();
    }

    /**
     * Method to get a color object.
     *
     * @param color
     * @return
     */
    public Color getColor(String color) {
        Matcher match = Pattern.compile("(\\d{1,3}),?\\s?(\\d{1,3}),?\\s?(\\d{1,3})").matcher(color);

        if (match.find()) {
            return Color.of(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)), Integer.parseInt(match.group(3)));
        } else {
            switch (color) {
                case "black":
                    return Color.BLACK;
                case "blue":
                    return Color.BLUE;
                case "cyan":
                    return Color.CYAN;
                case "gray":
                    return Color.GRAY;
                case "green":
                    return Color.GREEN;
                case "magenta":
                    return Color.MAGENTA;
                case "orange":
                    return Color.ORANGE;
                case "pink":
                    return Color.PINK;
                case "red":
                    return Color.RED;
                case "white":
                    return Color.WHITE;
                case "yellow":
                    return Color.YELLOW;
                default:
                    return Color.GRAY;
            }
        }
    }

    /**
     * Method to return a message by its id.
     *
     * @param channelName
     * @param messageId
     * @return
     */
    public Message getMessageById(String channelName, String messageId) {
        GuildMessageChannel channel = this.getChannelAsync(channelName, c -> {
            return c.getType() != Type.GUILD_CATEGORY;
        }).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
        if (channel != null) {
            return channel.getMessageById(Snowflake.of(messageId)).block();
        }

        return null;
    }

    /**
     * Method to edit message content.
     *
     * @param message
     * @param newMessage
     */
    public void editMessageContent(Message message, String newMessage) {
        message.edit(MessageEditSpec.create().withContentOrNull(newMessage)).subscribe();
    }

    /**
     * Method to edit message embeds.
     *
     * @param message
     * @param newEmbed
     */
    public void editMessageEmbed(Message message, EmbedCreateSpec newEmbed) {
        message.edit(MessageEditSpec.create().withEmbeds(newEmbed)).subscribe();
    }

    /**
     * Method to return a list of all messages before the given message.
     *
     * @param channelName
     * @param messageId
     * @return
     */
    public List<Message> getMessagesBefore(String channelName, String messageId) {
        GuildMessageChannel channel = this.getChannelAsync(channelName, c -> {
            return c.getType() != Type.GUILD_CATEGORY;
        }).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
        List<Message> messageList = new ArrayList<>();
        if (channel != null) {
            channel.getMessagesBefore(Snowflake.of(messageId)).toIterable().forEach(message -> messageList.add(message));
        }
        return messageList;
    }

    /**
     * Method to return a the last message of a given channel.
     *
     * @param channelName
     * @return
     */
    public Message getLastMessage(String channelName) {
        GuildMessageChannel channel = this.getChannelAsync(channelName).doOnError(e -> com.gmt2001.Console.err.printStackTrace(e)).block();
        if (channel != null) {
            return channel.getLastMessage().block();
        }

        return null;
    }

    public static Optional<Snowflake> channelId(Channel channel) {
        if (null != channel.getType()) {
            switch (channel.getType()) {
                case DM:
                case GROUP_DM:
                    return Optional.of(((PrivateChannel) channel).getId());
                case GUILD_CATEGORY:
                    return Optional.of(((Category) channel).getId());
                case GUILD_NEWS:
                    return Optional.of(((NewsChannel) channel).getId());
                case GUILD_STAGE_VOICE:
                    return Optional.of(((VoiceChannel) channel).getId());
                case GUILD_STORE:
                    return Optional.of(((StoreChannel) channel).getId());
                case GUILD_TEXT:
                    return Optional.of(((TextChannel) channel).getId());
                case GUILD_VOICE:
                    return Optional.of(((VoiceChannel) channel).getId());
                default:
                    break;
            }
        }

        return Optional.empty();
    }

    public static String channelIdAsString(Channel channel) {
        Optional<Snowflake> channelId = DiscordUtil.channelId(channel);

        if (channelId.isPresent()) {
            return channelId.get().asString();
        }

        return "";
    }

    public static String channelName(Channel channel) {
        if (null != channel.getType()) {
            switch (channel.getType()) {
                case DM:
                case GROUP_DM:
                    return ((PrivateChannel) channel).getMention();
                case GUILD_CATEGORY:
                    return ((Category) channel).getName();
                case GUILD_NEWS:
                    return ((NewsChannel) channel).getName();
                case GUILD_STAGE_VOICE:
                    return ((VoiceChannel) channel).getName();
                case GUILD_STORE:
                    return ((StoreChannel) channel).getName();
                case GUILD_TEXT:
                    return ((TextChannel) channel).getName();
                case GUILD_VOICE:
                    return ((VoiceChannel) channel).getName();
                default:
                    break;
            }
        }

        return "";
    }

    public class MessageCreateFile implements MessageCreateFields.File {

        private final String fileName;
        private final String fileLocation;

        public MessageCreateFile(String fileName, String fileLocation) {
            this.fileName = fileName;
            this.fileLocation = fileLocation;
        }

        @Override
        public String name() {
            return this.fileName;
        }

        public String path() {
            return this.fileLocation;
        }

        @Override
        public InputStream inputStream() {
            try {
                return Files.newInputStream(Paths.get(this.fileLocation));
            } catch (IOException ex) {
                com.gmt2001.Console.err.printStackTrace(ex);
            }

            return null;
        }
    }
}
