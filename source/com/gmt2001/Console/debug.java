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
package com.gmt2001.Console;

import com.illusionaryone.Logger;
import com.gmt2001.RollbarProvider;
import com.gmt2001.util.LogFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import tv.phantombot.PhantomBot;

public final class debug {

    private debug() {
    }

    static String findCallerInfo() {
        StackTraceElement st = findCaller();

        return "[" + st.getMethodName() + "()@" + st.getFileName() + ":" + st.getLineNumber() + "]";
    }

    static StackTraceElement findCaller() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (StackTraceElement st1 : st) {
            if (!st1.getClassName().startsWith("com.gmt2001.Console") && !st1.getMethodName().startsWith("getStackTrace")
                    && !st1.getClassName().startsWith("reactor.")) {
                return st1;
            }
        }

        return st.length >= 4 ? st[3] : st[0];
    }

    public static String findCallerInfo(String myClassName) {
        StackTraceElement st = findCaller(myClassName);

        return "[" + st.getMethodName() + "()@" + st.getFileName() + ":" + st.getLineNumber() + "]";
    }

    public static StackTraceElement findCaller(String myClassName) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        StackTraceElement foundme = null;

        for (StackTraceElement st1 : st) {
            if (st1.getClassName().startsWith(myClassName)) {
                foundme = st1;
            } else if (foundme != null && !st1.getMethodName().startsWith("getStackTrace") && !st1.getClassName().startsWith("reactor.")) {
                return st1;
            }
        }

        return foundme != null ? foundme : st[0];
    }

    public static void println() {
        if (PhantomBot.getEnableDebugging()) {
            Logger.instance().log(Logger.LogType.Debug, "");
            if (!PhantomBot.getEnableDebuggingLogOnly()) {
                System.out.println();
            }
        }
    }

    public static void printlnRhino(Object o) {
        if (PhantomBot.getEnableDebugging()) {
            loglnRhino(o);
            if (!PhantomBot.getEnableDebuggingLogOnly()) {
                System.out.println("[" + logTimestamp.log() + "] [DEBUG] " + o);
            }
        }
    }

    public static void loglnRhino(Object o) {
        if (PhantomBot.getEnableDebugging()) {
            Logger.instance().log(Logger.LogType.Debug, "[" + logTimestamp.log() + "] " + LogFilter.filter(o.toString()));
            Logger.instance().log(Logger.LogType.Debug, "");
        }
    }

    public static void println(Object o) {
        println(o, false);
    }

    public static void println(Object o, boolean force) {
        println(o, force, LogFilter.filter(o.toString()));
    }

    public static void println(Object o, String log) {
        println(o, false, log);
    }

    public static void println(Object o, boolean force, Object log) {
        if (PhantomBot.getEnableDebugging() || force) {
            String stackInfo = findCallerInfo() + " ";
            Logger.instance().log(Logger.LogType.Debug, "[" + logTimestamp.log() + "] " + stackInfo + log.toString());
            Logger.instance().log(Logger.LogType.Debug, "");

            if (!PhantomBot.getEnableDebuggingLogOnly()) {
                System.out.println("[" + logTimestamp.log() + "] [DEBUG] " + stackInfo + o);
            }
        }
    }

    public static void logln(Object o) {
        logln(o, false);
    }

    public static void logln(Object o, boolean force) {
        if (PhantomBot.getEnableDebugging() || force) {
            Logger.instance().log(Logger.LogType.Debug, "[" + logTimestamp.log() + "] " + findCallerInfo() + " " + o.toString());
            Logger.instance().log(Logger.LogType.Debug, "");
        }
    }

    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return null;
        }

        try ( Writer trace = new StringWriter()) {
            try ( PrintWriter ptrace = new PrintWriter(trace)) {

                e.printStackTrace(ptrace);

                return trace.toString();
            }
        } catch (IOException ex) {
            err.printStackTrace(ex);
        }

        return null;
    }

    public static void printStackTrace(Throwable e) {
        printStackTrace(e, "");
    }

    public static void printStackTrace(Throwable e, String description) {
        printStackTrace(e, null, description);
    }

    public static void printStackTrace(Throwable e, Map<String, Object> custom) {
        printStackTrace(e, custom, "");
    }

    public static void printStackTrace(Throwable e, Map<String, Object> custom, String description) {
        if (PhantomBot.getEnableDebugging()) {
            System.err.println(findCallerInfo());
            e.printStackTrace(System.err);
        }

        logStackTrace(e, custom, description);
    }

    /**
     * Prints the stack trace if debug is enabled and not in log only mode
     *
     * Always logs the stack trace, even with debug off
     *
     * @param e A {@link Throwable} to print/log
     */
    public static void printOrLogStackTrace(Throwable e) {
        if (PhantomBot.getEnableDebugging()) {
            if (!PhantomBot.getEnableDebuggingLogOnly()) {
                printStackTrace(e);
            } else {
                logStackTrace(e);
            }
        }
    }

    public static void logStackTrace(Throwable e) {
        logStackTrace(e, "");
    }

    public static void logStackTrace(Throwable e, String description) {
        logStackTrace(e, null, description);
    }

    public static void logStackTrace(Throwable e, Map<String, Object> custom) {
        logStackTrace(e, custom, "");
    }

    public static void logStackTrace(Throwable e, Map<String, Object> custom, String description) {
        if (PhantomBot.getEnableDebugging()) {
            if (custom == null) {
                custom = new HashMap<>();
            }

            String stackInfo = findCallerInfo();

            try {
                custom.putIfAbsent("__caller", stackInfo);
            } catch (UnsupportedOperationException ex) {
                Map<String, Object> oldCustom = custom;
                custom = new HashMap<>(oldCustom);
                custom.putIfAbsent("__caller", stackInfo);
            }

            RollbarProvider.instance().debug(e, custom, description);

            try ( Writer trace = new StringWriter()) {
                try ( PrintWriter ptrace = new PrintWriter(trace)) {

                    e.printStackTrace(ptrace);

                    Logger.instance().log(Logger.LogType.Debug, "[" + logTimestamp.log() + "] " + stackInfo);
                    Logger.instance().log(Logger.LogType.Debug, trace.toString());
                    Logger.instance().log(Logger.LogType.Debug, "");
                }
            } catch (IOException ex) {
                err.printStackTrace(ex);
            }
        }
    }
}
