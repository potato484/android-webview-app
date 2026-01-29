#!/bin/bash
set -euo pipefail

GRADLE_VERSION="8.2"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WRAPPER_PROPS="${SCRIPT_DIR}/gradle/wrapper/gradle-wrapper.properties"
GRADLE_DISTS_DIR="${HOME}/.gradle/wrapper/dists"
GRADLE_DIR="${GRADLE_DISTS_DIR}/gradle-${GRADLE_VERSION}"

read_distribution_url() {
    if [ -f "$WRAPPER_PROPS" ]; then
        # Example in gradle-wrapper.properties: https\://... -> https://...
        local url
        url="$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPS" | head -n 1)"
        url="${url//\\/}"
        if [ -n "$url" ]; then
            printf '%s' "$url"
            return 0
        fi
    fi
    printf '%s' "https://mirrors.cloud.tencent.com/gradle/gradle-${GRADLE_VERSION}-bin.zip"
}

if [ ! -x "${GRADLE_DIR}/bin/gradle" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_DISTS_DIR"
    TMP_ZIP="$(mktemp "/tmp/gradle-${GRADLE_VERSION}-bin.XXXXXX.zip")"
    cleanup() { rm -f "$TMP_ZIP"; }
    trap cleanup EXIT
    DIST_URL="${GRADLE_DISTRIBUTION_URL:-$(read_distribution_url)}"
    curl --fail --location --retry 3 --retry-all-errors --connect-timeout 10 --progress-bar \
        "$DIST_URL" -o "$TMP_ZIP"
    unzip -oq "$TMP_ZIP" -d "$GRADLE_DISTS_DIR"
fi

exec "${GRADLE_DIR}/bin/gradle" "$@"
