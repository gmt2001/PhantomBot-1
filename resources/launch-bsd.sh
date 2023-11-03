#!/bin/sh
#
# Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

#
# PhantomBot Launcher - BSD
#
# Please run the following to launch the bot, the chmod is required only once.
# % chmod +x launch-bsd.sh
# % ./launch-bsd.sh
#
# You can also specify a custom path to the Java executable as the first parameter, using java=PATH
# ex: ./launch-bsd.sh java=/usr/local/jdk-21/bin/java
#

# Required Java major version
javarequired=21

unset DISPLAY

JAVA=$(which /usr/local/*$javarequired*/bin/java 2>/dev/null)

if [ "$JAVA" -eq "" ]; then
    JAVA=$(which java)
fi

if [ -e "$1" ]; then
    n=0
    IFS="="
    for v in $1
    do
        n=`expr $n + 1`
        if expr "$v" : '.* .*' > /dev/null
        then
            z="var$n=\"$v\""
        else
            z="var$n=$v"
        fi
        eval "$z"
    done

    if [ "$var1" =~ "java" && -e "$var2" ]; then
        JAVA="$var2"
        $1=$2
    fi
fi

jvermaj=$($JAVA --version | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)

if [ $jvermaj -lt $javarequired ]; then
    echo "PhantomBot requires Java $javarequired or later to run."
    echo

    osdist=$(uname)

    if  [[ "$osdist" == *"OpenBSD"* ]]; then
        echo "Please install the package jdk and select the version that gives you jdk-$javarequired"
        echo

        echo "The command to do this is:"
        echo "   pkg_add jdk"
        echo
        echo "When you issue the pkg_add command, select the option that starts with java-$javarequired"
    elif  [[ "$osdist" == *"FreeBSD"* ]]; then
        echo "Please install the package openjdk$javarequired"
        echo

        echo "The command to do this is:"
        echo "   sudo pkg install openjdk$javarequired"
    else
        echo "Unknown OS detected, please identify and install the appropriate Java $javarequired package on your system"
        echo "The name is generally something starting with JDK or OpenJDK"
    fi

    echo
    echo "If you have already installed it, try specifying the path to the java executable as a parameter to this script"
    echo "Example: ./launch-bsd.sh java=/usr/local/jdk-$javarequired/bin/java"

    exit 1
fi

touch java.opt.custom

${JAVA} @java.opt -Dinteractive @java.opt.custom -jar PhantomBot.jar "$@"
