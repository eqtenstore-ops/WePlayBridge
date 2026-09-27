#!/usr/bin/env sh
# Minimal working gradlew - downloads from official
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi
exec "$JAVACMD" -Dorg.gradle.appname="Gradle" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
