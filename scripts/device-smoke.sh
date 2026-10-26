#!/usr/bin/env bash
# Smoke test of the gallery app on an Android emulator or an iOS simulator: builds and installs the app, opens
# the overview and then every catalog entry in turn, takes a screenshot of each, and reports pages that crash
# the app or come up blank. macOS only (it reads screenshots with sips).
#
#   scripts/device-smoke.sh android [options] [entry-id ...]
#   scripts/device-smoke.sh ios     [options] [entry-id ...]
#
# Options:
#   --no-build      install what was built last instead of building first
#   --light         open the pages in the light theme (default: dark)
#   --wait <s>      seconds to let each page settle before the screenshot (default: 2)
#   --out <dir>     where screenshots go (default: build/device-smoke/<platform>)
#
# Without entry ids it runs every id in samples/gallery's Catalog.kt. The overview runs first as "overview".
#
# Android: uses $ANDROID_SERIAL if set, otherwise the first running emulator (never a physical device unless
# ANDROID_SERIAL names it). Pages open through the activity's `entry` and `dark` intent extras.
# iOS: uses $IOS_SIMULATOR (a UDID) if set, otherwise the "iPhone 17 Pro" simulator of the newest runtime that
# has one ($IOS_DEVICE overrides the name); boots it if needed. Pages open through the `-entry` and `-dark`
# launch arguments. Needs xcodegen.
#
# Exit status: 0 when every page opened without a crash and drew something, 1 otherwise.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CATALOG="$ROOT/samples/gallery/src/commonMain/kotlin/com/jantursky/stipple/samples/gallery/Catalog.kt"
APP_ID="com.jantursky.stipple.samples.gallery"
ACTIVITY="com.jantursky.stipple.samples.android.MainActivity"

platform="${1:-}"
case "$platform" in
android | ios) shift ;;
*)
    sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac

build=1
dark=1
wait_s=2
out="$ROOT/build/device-smoke/$platform"
ids=()
while [[ $# -gt 0 ]]; do
    case "$1" in
    --no-build) build=0 ;;
    --light) dark=0 ;;
    --wait) wait_s="$2" && shift ;;
    --out) out="$2" && shift ;;
    *) ids+=("$1") ;;
    esac
    shift
done
if [[ ${#ids[@]} -eq 0 ]]; then
    # Every Page( in the catalog starts with its id on the next line.
    ids=(overview $(awk '/^ *Page\($/ { getline; gsub(/[ ",]/, ""); print }' "$CATALOG"))
fi
mkdir -p "$out"

# Prints "blank" when the middle of the screenshot (the stage on an entry page) is one flat color, else the
# spread of its luminance. The screenshot is shrunk to a small BMP with sips and read with plain Python.
check_blank() {
    local png="$1" bmp
    bmp="$(mktemp -t smoke).bmp"
    sips -s format bmp --resampleWidth 60 "$png" --out "$bmp" >/dev/null 2>&1
    python3 -I - "$bmp" <<'PY'
import struct, sys, statistics
data = open(sys.argv[1], "rb").read()
offset, = struct.unpack_from("<I", data, 10)
width, height = struct.unpack_from("<ii", data, 18)
bpp, = struct.unpack_from("<H", data, 28)
step = bpp // 8
row = (width * step + 3) & ~3
flip = height > 0
height = abs(height)
lum = []
# Rows 20 % to 60 % from the top, columns 10 % to 90 %: below the top bar, where the stage sits.
for y in range(int(height * 0.2), int(height * 0.6)):
    ry = height - 1 - y if flip else y
    for x in range(int(width * 0.1), int(width * 0.9)):
        b, g, r = data[offset + ry * row + x * step: offset + ry * row + x * step + 3]
        lum.append(0.2126 * r + 0.7152 * g + 0.0722 * b)
spread = statistics.pstdev(lum)
print("blank" if spread < 1.5 else "%.1f" % spread)
PY
    rm -f "$bmp"
}

failures=()
report() {
    local id="$1" status="$2" detail="$3"
    printf '%-34s %-8s %s\n' "$id" "$status" "$detail"
    [[ "$status" == ok ]] || failures+=("$id")
}

# Reports [id] as drawn or blank from its screenshot. A transition that hides its content first is empty for a
# moment, so a blank page gets a second screenshot 1.5 s later and is blank only if that one is too.
# Expects a capture function for the platform that writes a screenshot to the path it is given.
judge() {
    local id="$1" spread
    spread="$(check_blank "$out/$id.png")"
    if [[ "$spread" == blank ]]; then
        sleep 1.5
        capture "$out/$id-later.png"
        spread="$(check_blank "$out/$id-later.png")"
    fi
    if [[ "$spread" == blank ]]; then report "$id" blank "$out/$id.png"; else report "$id" ok "spread $spread"; fi
}

run_android() {
    local serial="${ANDROID_SERIAL:-}"
    if [[ -z "$serial" ]]; then
        serial="$(adb devices | awk '/^emulator-[0-9]+\tdevice$/ { print $1; exit }')"
    fi
    if [[ -z "$serial" ]]; then
        echo "No running emulator. Start one, e.g. emulator -avd Medium_Phone -no-snapshot -no-audio &, or set ANDROID_SERIAL." >&2
        exit 2
    fi
    local adb=(adb -s "$serial")
    capture() { adb -s "$serial" exec-out screencap -p >"$1"; }
    echo "Android device $serial, API $("${adb[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
    if [[ $build -eq 1 ]]; then
        "$ROOT/gradlew" -p "$ROOT" :samples:androidApp:assembleDebug -q
    fi
    "${adb[@]}" install -r "$ROOT/samples/androidApp/build/outputs/apk/debug/androidApp-debug.apk" >/dev/null
    local dark_arg=true
    [[ $dark -eq 1 ]] || dark_arg=false
    for id in "${ids[@]}"; do
        local extras=(--ez dark "$dark_arg")
        [[ "$id" == overview ]] || extras+=(--es entry "$id")
        "${adb[@]}" logcat -c
        "${adb[@]}" shell am start -S -W -n "$APP_ID/$ACTIVITY" "${extras[@]}" >/dev/null
        sleep "$wait_s"
        capture "$out/$id.png"
        local crash pid
        # Only this app's crashes: other apps on the device can crash in the background meanwhile.
        crash="$("${adb[@]}" logcat -d -b crash 2>/dev/null | grep -A1 -F "Process: $APP_ID," | tail -1 || true)"
        pid="$("${adb[@]}" shell pidof "$APP_ID" | tr -d '\r' || true)"
        if [[ -n "$crash" || -z "$pid" ]]; then
            "${adb[@]}" logcat -d >"$out/$id.log"
            report "$id" crash "${crash:-process gone} (log: $out/$id.log)"
        else
            judge "$id"
        fi
    done
    "${adb[@]}" shell am force-stop "$APP_ID"
}

run_ios() {
    command -v xcodegen >/dev/null || {
        echo "xcodegen is missing: brew install xcodegen" >&2
        exit 2
    }
    local udid="${IOS_SIMULATOR:-}" name="${IOS_DEVICE:-iPhone 17 Pro}"
    if [[ -z "$udid" ]]; then
        # The runtimes print oldest first, so the last match is the newest.
        udid="$(xcrun simctl list devices available | grep -F "    $name (" | tail -1 | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/')"
    fi
    if [[ -z "$udid" ]]; then
        echo "No simulator named $name. Set IOS_SIMULATOR to a UDID from xcrun simctl list devices." >&2
        exit 2
    fi
    xcrun simctl bootstatus "$udid" -b >/dev/null
    capture() { xcrun simctl io "$udid" screenshot "$1" >/dev/null 2>&1; }
    echo "iOS simulator $udid: $(xcrun simctl list devices | grep -F "$udid" | sed -E 's/^ *//')"
    local derived="$ROOT/samples/iosApp/build"
    if [[ $build -eq 1 ]]; then
        (cd "$ROOT/samples/iosApp" && xcodegen generate --quiet)
        xcodebuild -project "$ROOT/samples/iosApp/GalleryApp.xcodeproj" -scheme GalleryApp -configuration Debug \
            -sdk iphonesimulator -destination "id=$udid" -derivedDataPath "$derived" build -quiet
    fi
    xcrun simctl install "$udid" "$derived/Build/Products/Debug-iphonesimulator/GalleryApp.app"
    local dark_arg=YES
    [[ $dark -eq 1 ]] || dark_arg=NO
    local reports="$HOME/Library/Logs/DiagnosticReports"
    for id in "${ids[@]}"; do
        local args=(-dark "$dark_arg")
        [[ "$id" == overview ]] || args+=(-entry "$id")
        local marker
        marker="$(mktemp -t smoke)"
        xcrun simctl launch --terminate-running-process "$udid" "$APP_ID" "${args[@]}" >/dev/null
        sleep "$wait_s"
        capture "$out/$id.png"
        local running crash
        running="$(xcrun simctl spawn "$udid" launchctl list | grep -F "$APP_ID" || true)"
        crash="$(find "$reports" -name 'GalleryApp*' -newer "$marker" 2>/dev/null | head -1)"
        rm -f "$marker"
        if [[ -n "$crash" || -z "$running" ]]; then
            report "$id" crash "${crash:-process gone}"
        else
            judge "$id"
        fi
    done
    xcrun simctl terminate "$udid" "$APP_ID" >/dev/null 2>&1 || true
}

"run_$platform"
echo
echo "Screenshots: $out"
if [[ ${#failures[@]} -gt 0 ]]; then
    echo "Failed (${#failures[@]}): ${failures[*]}"
    exit 1
fi
echo "All ${#ids[@]} pages opened and drew."
