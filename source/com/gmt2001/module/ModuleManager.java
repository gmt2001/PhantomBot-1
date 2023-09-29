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
package com.gmt2001.module;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gmt2001.datastore2.Datastore2;
import com.gmt2001.httpclient.URIUtil;
import com.gmt2001.module.meta.ModuleStatusTable;
import com.gmt2001.util.Reflect;

import net.engio.mbassy.listener.Handler;
import tv.phantombot.PhantomBot;
import tv.phantombot.event.Event;
import tv.phantombot.event.EventBus;
import tv.phantombot.event.Listener;
import tv.phantombot.event.command.CommandEvent;
import tv.phantombot.event.discord.channel.DiscordChannelCommandEvent;
import tv.phantombot.event.irc.message.IrcModerationEvent;
import tv.phantombot.event.webserver.WebServerMessageEvent;

/**
 * Loads and manages {@link Module}
 *
 * @author gmt2001
 */
public final class ModuleManager implements Listener {
    /**
     * Instance
     */
    private static final ModuleManager INSTANCE = new ModuleManager();
    /**
     * Available modules
     */
    private final Map<Class<? extends Module>, Module> modules = new ConcurrentHashMap<>();

    /**
     * Provides an instance of ModuleManager
     *
     * @return the instance
     */
    public static ModuleManager instance() {
        return INSTANCE;
    }

    /**
     * Generates a custom data map for Rollbar
     *
     * @param c the class object of the failing module
     * @param failLocation the method call where the throwable was caught
     * @return the data map
     */
    private static Map<String, Object> failMap(Class<? extends Module> c, String failLocation) {
        return Map.of(
            "module", c.getName(),
            "moduleType", CoreModule.class.isAssignableFrom(c) ? CoreModule.class.getSimpleName() : Module.class.getSimpleName(),
            "failLocation", failLocation
        );
    }

    /**
     * Constructor
     * <p>
     * Finds all modules and performs loading
     */
    private ModuleManager() {
        com.gmt2001.Console.out.println("Searching for custom Java modules...");
        try (Stream<Path> entries = Files.walk(Paths.get("./modules"), FileVisitOption.FOLLOW_LINKS)) {
            entries.filter(e -> Files.isRegularFile(e) && e.getFileName().toString().toLowerCase().endsWith(".jar")).map(e -> {
                try {
                    return URIUtil.create("file://" + e.toString()).toURL();
                } catch (MalformedURLException ex) {
                    com.gmt2001.Console.err.println("Failed to prep module file " + e.toString());
                    com.gmt2001.Console.err.printStackTrace(ex, Map.of("file", e.toString()));
                    return null;
                }
            }).filter(e -> e != null).forEach(e -> Reflect.instance().loadPackageRecursive(e));
        } catch (IOException ex) {
            com.gmt2001.Console.err.printStackTrace(ex);
        }

        com.gmt2001.Console.out.println("Loading core Java modules...");

        Reflect.instance().loadPackageRecursive(ModuleManager.class.getName()
                    .substring(0, ModuleManager.class.getName().lastIndexOf('.')))
                    .loadPackageRecursive(PhantomBot.class.getName()
                    .substring(0, PhantomBot.class.getName().lastIndexOf('.')) + ".scripts")
                .getSubTypesOf(CoreModule.class).stream().forEach(c -> {
                    CoreModule m;
                    try {
                        m = c.getConstructor().newInstance();
                        Module r = this.modules.putIfAbsent(c, m);
                        if (r != null) {
                            return;
                        }
                    } catch (Throwable e) {
                        com.gmt2001.Console.err.println("    Failed to construct Java core module " + c.getName());
                        com.gmt2001.Console.err.printStackTrace(e, failMap(c, "ctor"));
                        return;
                    }

                    try {
                        m.onLoad();
                        com.gmt2001.Console.out.println("    Loaded Java core module " + c.getName());
                    } catch (Throwable e) {
                        com.gmt2001.Console.err.println("    Failed to onLoad Java core module " + c.getName());
                        com.gmt2001.Console.err.printStackTrace(e, failMap(c, "onLoad"));
                    }
                });

        com.gmt2001.Console.out.println("Running afterCoreLoad...");

        this.modules.entrySet().stream().filter(e -> CoreModule.class.isAssignableFrom(e.getValue().getClass()))
            .map(e -> (CoreModule)e.getValue()).sorted((e1, e2) -> Integer.compare(e1.afterCoreLoadOrder(), e2.afterCoreLoadOrder()))
            .forEachOrdered(m -> {
                try {
                    m.afterCoreLoad();
                } catch (Throwable e) {
                    com.gmt2001.Console.err.println("    Failed to afterCoreLoad Java core module " + m.getClass().getName());
                    com.gmt2001.Console.err.printStackTrace(e, failMap(m.getClass(), "afterCoreLoad"));
                }
            });

        com.gmt2001.Console.out.println("Loading Java modules...");

        Reflect.instance().getSubTypesOf(Module.class).stream()
            .filter(e -> !CoreModule.class.isAssignableFrom(e.getClass())).forEach(c -> {
                    Module m;
                    try {
                        m = c.getConstructor().newInstance();
                        Module r = this.modules.putIfAbsent(c, m);
                        if (r != null) {
                            return;
                        }
                    } catch (Throwable e) {
                        com.gmt2001.Console.err.println("    Failed to construct Java module " + c.getName());
                        com.gmt2001.Console.err.printStackTrace(e, failMap(c, "ctor"));
                        return;
                    }

                    try {
                        m.onLoad();
                        com.gmt2001.Console.out.println("    Loaded Java module " + c.getName());
                    } catch (Throwable e) {
                        com.gmt2001.Console.err.println("    Failed to onLoad Java module " + c.getName());
                        com.gmt2001.Console.err.printStackTrace(e, failMap(c, "onLoad"));
                    }
                });

        com.gmt2001.Console.out.println("Running afterLoad...");

        this.modules.entrySet().stream().forEachOrdered(m -> {
                try {
                    m.getValue().afterLoad();
                } catch (Throwable e) {
                    com.gmt2001.Console.err.println("    Failed to afterLoad Java module " + m.getKey().getName());
                    com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "afterLoad"));
                }
            });

        com.gmt2001.Console.out.println("Initializing enabled state of Java modules...");

        List<String> moduleSet = this.modules.keySet().stream().map(e -> e.getName()).collect(Collectors.toList());

        final Map<String, Boolean> recordSet = Datastore2.instance().dslContext()
            .fetch(ModuleStatusTable.instance(), ModuleStatusTable.instance().MODULE.in(moduleSet))
            .intoMap(ModuleStatusTable.instance().MODULE, ModuleStatusTable.instance().ENABLED);

        this.modules.entrySet().stream().forEachOrdered(m -> {
                try {
                    boolean enabled = recordSet.getOrDefault(m.getKey().getName(), m.getValue().defaultEnabledState());

                    if (enabled) {
                        m.getValue().onEnable();
                    } else {
                        m.getValue().onDisable();
                    }

                    com.gmt2001.Console.out.println("    " + m.getKey().getName() + " => " + (enabled ? "Enabled" : "Disabled"));
                } catch (Throwable e) {
                    com.gmt2001.Console.err.println("    Failed to enable/disable Java module " + m.getKey().getName());
                    com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "enableDisableInit"));
                }
            });

        EventBus.instance().register(this);
    }

    /**
     * Retrives an unmodifiable view of the loaded modules
     *
     * @return the unmodifiable map of loaded modules
     */
    public Map<Class<? extends Module>, Module> modules() {
        return Collections.unmodifiableMap(this.modules);
    }

    /**
     * Retrives an instance of the specified module
     *
     * @param <T> the type of the module
     * @param type a class object of the module type
     * @return the module instance; {@code null} if no module with the specified type has been loaded
     */
    @SuppressWarnings({"unchecked"})
    public <T extends Module> T module(Class<T> type) {
        return (T) this.modules.get(type);
    }

    /**
     * Receives events from the event bus and dispatches them to modules
     * <p>
     * {@link IrcModerationEvent}, {@link CommandEvent}, {@link DiscordChannelCommandEvent}, and {@link WebServerMessageEvent} are instead sent to their handler methods
     *
     * @param event the event data
     */
    @Handler
    public void onEvent(Event event) {
        if (event instanceof IrcModerationEvent ime) {
            this.onIrcModerationEvent(ime);
        } else if (event instanceof CommandEvent ce) {
            this.onCommandEvent(ce);
        } else if (event instanceof DiscordChannelCommandEvent dce) {
            this.onDiscordChannelCommandEvent(dce);
        } else if (event instanceof WebServerMessageEvent wse) {
            this.onWebServerMessageEvent(wse);
        } else {
            this.modules.entrySet().stream().forEachOrdered(m -> {
                try {
                    m.getValue().onEvent(event);
                } catch (Throwable e) {
                    com.gmt2001.Console.err.println("Failed to dispatch " + event.getClass().getSimpleName() + " to Java module " + m.getKey().getName());
                    com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "onEvent#" + event.getClass().getSimpleName()));
                }
            });
        }
    }

    /**
     * Handles processing and dispatching specific to {@link IrcModerationEvent}
     *
     * @param event the event data
     */
    public void onIrcModerationEvent(IrcModerationEvent event) {
        this.modules.entrySet().stream().forEachOrdered(m -> {
            try {
                m.getValue().onIrcModerationEvent(event);
            } catch (Throwable e) {
                com.gmt2001.Console.err.println("Failed to dispatch " + event.getClass().getSimpleName() + " to Java module " + m.getKey().getName());
                com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "onEvent#" + event.getClass().getSimpleName()));
            }
        });

        event.complete();
    }

    /**
     * Handles processing and dispatching specific to {@link CommandEvent}
     *
     * @param event the event data
     */
    public void onCommandEvent(CommandEvent event) {
        AtomicBoolean handled = new AtomicBoolean(false);
        this.modules.entrySet().stream().forEachOrdered(m -> {
            try {
                if (m.getValue().onCommandEvent(event)) {
                    handled.set(true);
                }
            } catch (Throwable e) {
                com.gmt2001.Console.err.println("Failed to dispatch " + event.getClass().getSimpleName() + " to Java module " + m.getKey().getName());
                com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "onEvent#" + event.getClass().getSimpleName()));
            }
        });
    }

    /**
     * Handles processing and dispatching specific to {@link DiscordChannelCommandEvent}
     *
     * @param event the event data
     */
    public void onDiscordChannelCommandEvent(DiscordChannelCommandEvent event) {
        AtomicBoolean handled = new AtomicBoolean(false);
        this.modules.entrySet().stream().forEachOrdered(m -> {
            try {
                if (m.getValue().onDiscordChannelCommandEvent(event)) {
                    handled.set(true);
                }
            } catch (Throwable e) {
                com.gmt2001.Console.err.println("Failed to dispatch " + event.getClass().getSimpleName() + " to Java module " + m.getKey().getName());
                com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "onEvent#" + event.getClass().getSimpleName()));
            }
        });
    }

    /**
     * Handles processing and dispatching specific to {@link WebServerMessageEvent}
     *
     * @param event the event data
     */
    public void onWebServerMessageEvent(WebServerMessageEvent event) {
        this.modules.entrySet().stream().forEachOrdered(m -> {
            try {
                m.getValue().onWebServerMesageEvent(event);
            } catch (Throwable e) {
                com.gmt2001.Console.err.println("Failed to dispatch " + event.getClass().getSimpleName() + " to Java module " + m.getKey().getName());
                com.gmt2001.Console.err.printStackTrace(e, failMap(m.getKey(), "onEvent#" + event.getClass().getSimpleName()));
            }
        });
    }
}
