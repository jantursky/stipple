#!/usr/bin/env bash
# Frame-time benchmark of the gallery's `bench` page: three large animated dithers at a one-dp cell, drawn by the
# cell renderer or the shader renderer. Run it on a device, not in CI. macOS only.
#
#   scripts/frame-bench.sh android [options]    an emulator or a physical Android phone
#   scripts/frame-bench.sh ios     [options]    a physical iPhone (the simulator has no frame statistics)
#
# Options:
#   --renderer <cells|shader|both>   what the page draws with (default: both, one run each)
#   --seconds <n>                    how long each run measures (default: 10)
#   --repeat <n>                     runs per renderer; the summary reports the median run (default: 1)
#   --entry <id>                     gallery page to measure (default: bench). Its renderer only follows --renderer
#                                    on `bench`, `bench-blue-noise` and `bench-dots`; other pages ignore it
#   --no-build                       install what was built last instead of building first
#   --out <dir>                      where results go (default: build/frame-bench/<platform>/<timestamp>)
#
# Android: uses $ANDROID_SERIAL if set, otherwise the first attached device or running emulator. Frames come from
# `dumpsys gfxinfo <app> framestats`: the time from the intended vsync to the frame being completed, for every
# frame the app drew during the run, with the first warm-up seconds dropped.
# iOS: needs $IOS_DEVICE_UDID (xcrun devicectl list devices), a signed build of samples/iosApp installed on it
# (--no-build is implied: build and install it from Xcode), and Xcode's `xctrace`. Frames come from the
# "Animation Hitches" Instruments template. UNVERIFIED on a physical iPhone: see docs and the task list of the
# change verify-docs-and-device-performance.
#
# Output, in <dir>: results.csv with one row per run, summary.md with the device, the build and the median run
# per renderer.
#
#   platform,device,os,refresh_hz,entry,renderer,run,frames,p50_ms,p90_ms,p95_ms,p99_ms,over_budget_pct
#
# over_budget_pct is the share of frames that took longer than one refresh interval.
#
# Exit status: 0 when every run produced frames, 1 otherwise, 2 for a missing device or tool.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_ID="com.jantursky.stipple.samples.gallery"
ACTIVITY="com.jantursky.stipple.samples.android.MainActivity"
WARMUP_S=3

platform="${1:-}"
case "$platform" in
android | ios) shift ;;
*)
    sed -n '2,32p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac

renderer=both
seconds=10
repeat=1
entry=bench
build=1
out=""
while [[ $# -gt 0 ]]; do
    case "$1" in
    --renderer) renderer="$2" && shift ;;
    --seconds) seconds="$2" && shift ;;
    --repeat) repeat="$2" && shift ;;
    --entry) entry="$2" && shift ;;
    --no-build) build=0 ;;
    --out) out="$2" && shift ;;
    *)
        echo "Unknown option $1" >&2
        exit 2
        ;;
    esac
    shift
done
case "$renderer" in
cells | shader) renderers=("$renderer") ;;
both) renderers=(cells shader) ;;
*)
    echo "--renderer is cells, shader or both" >&2
    exit 2
    ;;
esac
[[ -n "$out" ]] || out="$ROOT/build/frame-bench/$platform/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out"
csv="$out/results.csv"
echo "platform,device,os,refresh_hz,entry,renderer,run,frames,p50_ms,p90_ms,p95_ms,p99_ms,over_budget_pct" >"$csv"

# Reads frame durations in milliseconds, one per line, from stdin, and prints "frames p50 p90 p95 p99 over_pct" for
# the refresh interval in $1 (milliseconds).
stats() {
    python3 -I -c '
import sys
interval = float(sys.argv[1])
d = sorted(float(x) for x in sys.stdin.read().split())
if not d:
    print("0 0 0 0 0 0")
    sys.exit(0)
def pct(p):
    return d[min(len(d) - 1, int(round(p / 100 * (len(d) - 1))))]
over = 100.0 * sum(1 for x in d if x > interval) / len(d)
print(len(d), "%.2f %.2f %.2f %.2f %.1f" % (pct(50), pct(90), pct(95), pct(99), over))
' "$1"
}

failures=0
record() { # device os refresh renderer run "frames p50 p90 p95 p99 over"
    local device="$1" os="$2" refresh="$3" r="$4" run="$5" line="$6"
    # shellcheck disable=SC2086
    set -- $line
    printf '%-8s run %s: %6s frames  p50 %s  p90 %s  p95 %s  p99 %s ms  %s%% over budget\n' "$r" "$run" "$1" "$2" "$3" "$4" "$5" "$6"
    echo "$platform,\"$device\",$os,$refresh,$entry,$r,$run,$1,$2,$3,$4,$5,$6" >>"$csv"
    [[ "$1" -gt 0 ]] || failures=$((failures + 1))
}

run_android() {
    local serial="${ANDROID_SERIAL:-}"
    if [[ -z "$serial" ]]; then
        serial="$(adb devices | awk '$2 == "device" { print $1; exit }')"
    fi
    if [[ -z "$serial" ]]; then
        echo "No device. Attach a phone, start an emulator, or set ANDROID_SERIAL." >&2
        exit 2
    fi
    local adb=(adb -s "$serial")
    local model api refresh
    model="$("${adb[@]}" shell getprop ro.product.model | tr -d '\r')"
    api="$("${adb[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
    refresh="$("${adb[@]}" shell dumpsys display | tr -d '\r' | grep -o 'renderFrameRate [0-9.]*' | head -1 | awk '{ print $2 }' || true)"
    [[ -n "$refresh" ]] || refresh="$("${adb[@]}" shell dumpsys display | tr -d '\r' | grep -o 'fps=[0-9.]*' | head -1 | cut -d= -f2 || true)"
    [[ -n "$refresh" ]] || refresh=60
    local interval
    interval="$(python3 -I -c "print(1000.0 / float('$refresh'))")"
    echo "Android $model ($serial), API $api, ${refresh} Hz"
    if [[ $build -eq 1 ]]; then
        "$ROOT/gradlew" -p "$ROOT" :samples:androidApp:assembleDebug -q
    fi
    "${adb[@]}" install -r "$ROOT/samples/androidApp/build/outputs/apk/debug/androidApp-debug.apk" >/dev/null
    for r in "${renderers[@]}"; do
        for run in $(seq 1 "$repeat"); do
            "${adb[@]}" shell am start -S -W -n "$APP_ID/$ACTIVITY" --ez dark true --es entry "$entry" \
                --es renderer "$r" >/dev/null
            sleep "$WARMUP_S"
            "${adb[@]}" shell dumpsys gfxinfo "$APP_ID" reset >/dev/null
            sleep "$seconds"
            local durations
            # PROFILEDATA rows follow a header that names the columns, and newer Android versions add columns, so
            # IntendedVsync and FrameCompleted are looked up by name. A frame with flags set is not an ordinary
            # frame (for example a layout change) and is skipped, as Android's own tools do.
            durations="$("${adb[@]}" shell dumpsys gfxinfo "$APP_ID" framestats | tr -d '\r' | python3 -I -c '
import sys
on = False
names = None
for line in sys.stdin:
    line = line.strip()
    if line == "---PROFILEDATA---":
        on = not on
        continue
    if not on:
        continue
    c = line.rstrip(",").split(",")
    if c[0] == "Flags":
        names = {n: i for i, n in enumerate(c)}
        continue
    if names is None or c[0] != "0" or len(c) <= max(names["IntendedVsync"], names["FrameCompleted"]):
        continue
    start, done = int(c[names["IntendedVsync"]]), int(c[names["FrameCompleted"]])
    if start > 0 and done > start:
        print((done - start) / 1e6)
')"
            record "$model" "API $api" "$refresh" "$r" "$run" "$(stats "$interval" <<<"$durations")"
        done
    done
    "${adb[@]}" shell am force-stop "$APP_ID"
    device_name="$model"
    os_name="API $api"
    refresh_hz="$refresh"
}

run_ios() {
    local udid="${IOS_DEVICE_UDID:-}"
    if [[ -z "$udid" ]]; then
        echo "Set IOS_DEVICE_UDID to a physical iPhone (xcrun devicectl list devices)." >&2
        exit 2
    fi
    command -v xctrace >/dev/null || {
        echo "xctrace is missing: install Xcode." >&2
        exit 2
    }
    local info model os refresh=60
    info="$(xcrun devicectl device info details --device "$udid" 2>/dev/null || true)"
    model="$(sed -n 's/^ *marketingName: *//p' <<<"$info" | head -1)"
    os="$(sed -n 's/^ *osVersionNumber: *//p' <<<"$info" | head -1)"
    [[ -n "$model" ]] || model="iPhone $udid"
    echo "iOS $model ($udid), ${os:-unknown}, assuming ${refresh} Hz (set IOS_REFRESH_HZ to override)"
    refresh="${IOS_REFRESH_HZ:-$refresh}"
    local interval
    interval="$(python3 -I -c "print(1000.0 / float('$refresh'))")"
    for r in "${renderers[@]}"; do
        for run in $(seq 1 "$repeat"); do
            local trace="$out/$r-$run.trace"
            xcrun devicectl device process launch --device "$udid" --terminate-existing "$APP_ID" \
                -dark YES -entry "$entry" -renderer "$r" >/dev/null
            sleep "$WARMUP_S"
            xcrun xctrace record --template "Animation Hitches" --device "$udid" --attach "$APP_ID" \
                --time-limit "${seconds}s" --output "$trace" >/dev/null 2>&1 || true
            local table durations
            table="$out/$r-$run.xml"
            xcrun xctrace export --input "$trace" \
                --xpath '/trace-toc/run/data/table[@schema="hitches-frame-lifetimes"]' --output "$table" >/dev/null 2>&1 || true
            if [[ ! -s "$table" ]]; then
                echo "No frame-lifetime table in $trace. Tables present:" >&2
                xcrun xctrace export --input "$trace" --toc 2>/dev/null | grep -o 'schema="[^"]*"' | sort -u >&2 || true
            fi
            # Each row is a frame; the "duration" column is in nanoseconds.
            durations="$(python3 -I -c '
import sys, xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    sys.exit(0)
for row in root.iter("row"):
    for el in row:
        if el.tag == "duration" and el.text and el.text.strip().isdigit():
            print(int(el.text) / 1e6)
' "$table")"
            record "$model" "iOS ${os:-?}" "$refresh" "$r" "$run" "$(stats "$interval" <<<"$durations")"
        done
    done
    device_name="$model"
    os_name="iOS ${os:-?}"
    refresh_hz="$refresh"
}

device_name=""
os_name=""
refresh_hz=""
"run_$platform"

# summary.md: the median run (by p95) of each renderer, with what was measured.
python3 -I - "$csv" "$out/summary.md" "$ROOT" <<'PY'
import csv, subprocess, sys, statistics
rows = list(csv.DictReader(open(sys.argv[1])))
commit = subprocess.run(["git", "-C", sys.argv[3], "rev-parse", "--short", "HEAD"], capture_output=True, text=True).stdout.strip()
out = []
if rows:
    r0 = rows[0]
    out += [f"# Frame times: {r0['device']}", "",
            f"- Platform: {r0['platform']}, {r0['os']}, {r0['refresh_hz']} Hz",
            f"- Page: `{r0['entry']}`, commit `{commit}`", ""]
    out += ["| Renderer | Runs | Frames | p50 ms | p90 ms | p95 ms | p99 ms | Over budget % | p95 spread ms |", "|---|---|---|---|---|---|---|---|---|"]
    for name in dict.fromkeys(r["renderer"] for r in rows):
        mine = sorted((r for r in rows if r["renderer"] == name), key=lambda r: float(r["p95_ms"]))
        m = mine[len(mine) // 2]
        spread = float(mine[-1]["p95_ms"]) - float(mine[0]["p95_ms"])
        out.append(f"| {name} | {len(mine)} | {m['frames']} | {m['p50_ms']} | {m['p90_ms']} | {m['p95_ms']} | {m['p99_ms']} | {m['over_budget_pct']} | {spread:.2f} |")
    out += ["", "The row for each renderer is its median run by p95; the spread is the gap between its best and worst run, which is the noise to compare differences against. `results.csv` has every run."]
open(sys.argv[2], "w").write("\n".join(out) + "\n")
PY
echo
echo "Results: $out/results.csv and $out/summary.md"
if [[ $failures -gt 0 ]]; then
    echo "$failures run(s) produced no frames."
    exit 1
fi
