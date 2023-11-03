#!/usr/bin/env bash
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
# PhantomBot Launcher - Linux and macOS
#
# Please run the following to launch the bot, the chmod is required only once.
# % chmod +x launch.sh
# % ./launch.sh
#
# Usage available by using the --help flag
#

unset DISPLAY

##### Script Settings #######

# Required Java major version
javarequired=21

# Minimum Ubuntu version to support the required Java version
ubuntumin=18.04
ubuntuminname="bionic beaver"

# Minimum Debian version to support the required Java version
debianmin=11
debianminname="bullseye"

# Minimum RHEL/CentOS version to support the required Java version
rhelmin=9

# Minimum Fedora version to support the required Java version
fedoramin=37

#############################

# Determine the directory of the running script
pushd . > '/dev/null';
SCRIPT_PATH="${BASH_SOURCE[0]:-$0}";

while [ -h "$SCRIPT_PATH" ];
do
    cd "$( dirname -- "$SCRIPT_PATH"; )";
    SCRIPT_PATH="$( readlink -f -- "$SCRIPT_PATH"; )";
done

cd "$( dirname -- "$SCRIPT_PATH"; )" > '/dev/null';
SCRIPT_PATH="$( pwd; )";
popd  > '/dev/null'

# Switch to script directory
pushd "$SCRIPT_PATH"

# Internal vars
tmp=""
interactive="-Dinteractive"
hwname="$( uname -m )"
trylinux=0
trymac=0
tryarm64=0
tryarm32=0
daemon=0
myjava=0
JAVA=""

POSITIONAL_ARGS=()

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --daemon)
      daemon=1
      shift
      ;;
    --java)
      JAVA="$2"
      myjava=1
      shift
      shift
      ;;
    --help)
      echo "Usage: launch.sh [options] [args]"
      echo ""
      echo "Options:"
      echo "  --daemon                        Enables daemon mode (STDIN disabled)"
      echo "  --java </path/to/jre/bin/java>  Overrides the first Java executable attempted"
      echo "  args                            Arguments to pass to PhantomBot.jar"
      echo ""
      echo "JVM flags can be passed by editing the java.opt.custom file"
      exit 0
    ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}"

# Ensure running user has chown if daemon mode
# Also disable interactive switch
if (( daemon == 1 )); then
    interactive=""
    if [[ ! -O "PhantomBot.jar" ]]; then
        echo "The directory is not chown by the service user"
        echo "Please run the following command to fix this:"
        echo "   sudo chown ${EUID} ${SCRIPT_PATH}"

        exit 1
    fi
fi

# Determine which builtin runtimes to try
if [[ "$OSTYPE" =~ "darwin" ]]; then
    trymac=1

    if [[ "$(sysctl -n machdep.cpu.brand_string)" =~ "Apple" ]]; then
        tryarm64=1
    fi
fi
if [[ "$hwname" =~ "arm64" || "$hwname" =~ "aarch64" ]]; then
    tryarm64=1
elif [[ "$hwname" =~ "arm" ]]; then
    tryarm32=1
fi
if [[ "$hwname" =~ "x86_64" || "$MACHTYPE" =~ "x86_64" ]]; then
    trylinux=1
fi

success=0

# Try command line Java
if (( success == 0 && myjava == 1 )); then
    chm=$(chmod u+x $JAVA 2>/dev/null)
    jver=$($JAVA --version 2>/dev/null)
    res=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Try java-runtime-linux-amd64
if (( success == 0 && trylinux == 1 )); then
    JAVA="./java-runtime-linux-amd64/bin/java"
    chm=$(chmod u+x $JAVA 2>/dev/null)
    jver=$($JAVA --version 2>/dev/null)
    res=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Try java-runtime-macos-amd64
if (( success == 0 && trymac == 1 )); then
    JAVA="./java-runtime-macos-amd64/bin/java"
    chm=$(chmod u+x $JAVA 2>/dev/null)
    jver=$($JAVA --version 2>/dev/null)
    res=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Try java-runtime-linux-arm64
if (( success == 0 && tryarm64 == 1 )); then
    JAVA="./java-runtime-linux-arm64/bin/java"
    chm=$(chmod u+x $JAVA 2>/dev/null)
    jver=$($JAVA --version 2>/dev/null)
    res=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Try java-runtime-linux-arm
if (( success == 0 && tryarm32 == 1 )); then
    JAVA="./java-runtime-linux-arm/bin/java"
    chm=$(chmod u+x $JAVA 2>/dev/null)
    jver=$($JAVA --version 2>/dev/null)
    res=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Try system java
if (( success == 0 )); then
    JAVA=$(which java)
    res1=$?
    jver=$($JAVA --version 2>/dev/null)
    res2=$?
    jvermaj=$(echo "$jver" | awk 'FNR == 1 { print $2 }' | cut -d . -f 1)
    if (( res1 == 0 && res2 == 0 && jvermaj == javarequired )); then
        success=1
    fi
fi

# Print instructions if no Java satisfied requirements
if (( success == 0 )); then
    echo "PhantomBot requires Java ${javarequired} to run"
    echo
    echo "Eclipse Temurin by Adoptium is the officially supported JVM of PhantomBot"
    echo "Information about Adoptium is available at: https://adoptium.net"
    echo

    # macOS link to instructions
    if [[ "$OSTYPE" =~ "darwin" ]]; then
        echo "Please install the Eclipse Temurin ${javarequired} JRE package"
        echo
        echo "Instructions to install Temurin are at: https://adoptium.net/installation/macOS/"
        echo
        echo "NOTE: For Apple Silicon \(M1 or M2\) computers, download the aarch64 version"
    else
        # Linux instructions
        osdist=$(awk '/^ID(_LIKE)?=/' /etc/os-release | sed 's/"//g' | sort --field-separator== --key=1,1 --dictionary-order --reverse | cut -d = -f 2 | awk 'FNR == 1')
        osdist2=$(awk '/^ID(_LIKE)?=/' /etc/os-release | sed 's/"//g' | sort --field-separator== --key=1,1 --dictionary-order --reverse | cut -d = -f 2 | awk 'FNR == 2')
        osver=$(awk '/^VERSION_ID=/' /etc/os-release | sed 's/"//g' | cut -d = -f 2)

        echo "Please install the package temurin-${javarequired}-jdk"
        echo

       # Warn on old Ubuntu
        if [[ "$osdist" == *"ubuntu"* ]] && (( osver < ubuntumin )); then
            echo "WARNING: You are running an Ubuntu derivative lower than version ${ubuntumin} (${ubuntuminname})"
            echo "Java ${javarequired} may not be available on this version"
            echo "It is recommended to upgrade to at least Ubuntu ${ubuntumin} (${ubuntuminname})"
            echo "NOTE: Upgrading the major version of the OS usually means a clean install (wipe)"
            echo
        # Warn on old Debian
        elif [[ "$osdist" == *"debian"* ]] && (( osver < debianmin )); then
            echo "WARNING: You are running a Debian derivative lower than version ${debianmin} (${debianminname})"
            echo "Java ${javarequired} may not be available on this version"
            echo "It is recommended to upgrade to at least Debian ${debianmin} (${debianminname})"
            echo "NOTE: Upgrading the major version of the OS usually means a clean install (wipe)"
            echo
        # Warn on old RHEL/CentOS
        elif [[ "$osdist" == *"rhel"* || "$osdist2" == *"rhel"* ]] && (( osver < rhelmin )); then
            echo "WARNING: You are running a RHEL derivative lower than version ${rhelmin}"
            echo "Java ${javarequired} may not be available on this version"
            echo "It is recommended to upgrade to at least RHEL ${rhelmin}"
            echo "NOTE: Upgrading the major version of the OS usually means a clean install (wipe)"
            echo
        # Warn on old Fedora
        elif [[ "$osdist" == *"fedora"* ]] && (( osver < fedoramin )); then
            echo "WARNING: You are running a Fedora derivative lower than version ${fedoramin}"
            echo "Java ${javarequired} may not be available on this version"
            echo "It is recommended to upgrade to at least Fedora ${fedoramin}"
            echo "NOTE: Upgrading the major version of the OS usually means a clean install (wipe)"
            echo
        fi

        echo "Instructions to add the Eclipse Adoptium repository and install Temurin are at: https://adoptium.net/installation/linux/"
        echo
        echo "After installing Temurin, use this command to ensure it is the default Java installation:"
        echo "   sudo update-alternatives --config java"
        echo
        echo "When you issue the update-alternatives command, select the option for temurin-${javarequired}-jdk"
    fi

    exit 1
fi

if mount | grep '/tmp' | grep -q noexec; then
    mkdir -p ${SCRIPT_PATH}/tmp
    tmp="-Djava.io.tmpdir=${SCRIPT_PATH}/tmp"
fi

touch java.opt.custom

${JAVA} @java.opt ${tmp} ${interactive} @java.opt.custom -jar PhantomBot.jar "$@"
popd
