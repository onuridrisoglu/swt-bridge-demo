#!/usr/bin/env bash
# Builds and runs WarehousePro 3.5 as an SWT desktop application.
# The shaded application jar carries everything except SWT, which is taken
# from the local Maven repository (the platform's SWT jar).
# Extra JVM options can be passed as arguments, e.g.
#   ./start-swt.sh -Dwarehouse.autologin=admin
set -euo pipefail
cd "$(dirname "$0")"
MVN="${MVN:-mvn}"

"$MVN" -B -q install -DskipTests
"$MVN" -B -q -pl swt-app dependency:build-classpath \
    -DincludeGroupIds=org.eclipse.platform \
    -Dmdep.outputFile=target/swt-classpath.txt

APP_JAR=swt-app/target/warehouse-swt-app-3.5.0.jar
SWT_CP="$(cat swt-app/target/swt-classpath.txt)"

JVM_ARGS=()
if [[ "$(uname -s)" == "Darwin" ]]; then
    # Cocoa requires the SWT Display to live on the process's first thread.
    JVM_ARGS+=(-XstartOnFirstThread)
fi

# The H2 database (./warehousedb) is created in the working directory.
exec java --enable-native-access=ALL-UNNAMED "${JVM_ARGS[@]}" "$@" -cp "$APP_JAR:$SWT_CP" com.example.warehouse.swt.WarehouseApp
