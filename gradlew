#!/bin/sh

#
# Gradle start up script for POSIX
#

# Add default JVM options here
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Use the maximum available, or set MAX_FD != -1 to use that value
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
    CYGWIN* )
        cygwin=true
        ;;
    Darwin* )
        darwin=true
        ;;
    MSYS* | MINGW* )
        msys=true
        ;;
    NONSTOP* )
        nonstop=true
        ;;
esac

# Determine the Java command
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Increase the maximum file descriptors if possible
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in
        max*)
            MAX_FD=$(ulimit -H -n) ||
                warn "Could not query maximum file descriptor limit"
            ;;
    esac
    case $MAX_FD in
        '' | soft) ;;
        *)
            ulimit -n "$MAX_FD" ||
                warn "Could not set maximum file descriptor limit to $MAX_FD"
            ;;
    esac
fi

# Resolve script directory
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=$( ls -ld "$PRG" )
    link=$( expr "$ls" : '.*-> \(.*\)$' )
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$( dirname "$PRG" )/"$link"
    fi
done
APP_HOME=$( cd "$( dirname "$PRG" )" > /dev/null && pwd -P ) || exit

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command, following shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

exec "$JAVACMD" "$@"
