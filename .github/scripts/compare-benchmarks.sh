#!/usr/bin/env bash
# Compare two JMH benchmark result JSON files and output a Markdown summary.
# Usage: compare-benchmarks.sh <base.json> <head.json>
# Requires: jq, awk (both available on ubuntu-latest GitHub Actions runners)
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "Usage: $0 <base.json> <head.json>" >&2
    exit 1
fi

BASE_JSON="$1"
HEAD_JSON="$2"

# Parse a JMH JSON file into tab-separated lines: name, score, error, unit
extract_primary() {
    jq -r '.[] | [
        (.benchmark | split(".") | last),
        (.primaryMetric.score | tostring),
        (.primaryMetric.scoreError | tostring),
        .primaryMetric.scoreUnit
    ] | join("\t")' "$1"
}

# Parse GC allocation metric: name, score (or "null" when absent)
# The middle-dot (·) is JMH's metric namespace separator.
extract_gc() {
    jq -r '.[] | [
        (.benchmark | split(".") | last),
        (.secondaryMetrics["·gc.alloc.rate.norm"].score // "null" | tostring)
    ] | join("\t")' "$1"
}

declare -A B_SCORE B_ERROR B_UNIT H_SCORE H_ERROR H_UNIT B_GC H_GC

while IFS=$'\t' read -r n s e u; do
    B_SCORE[$n]=$s; B_ERROR[$n]=$e; B_UNIT[$n]=$u
done < <(extract_primary "$BASE_JSON")

while IFS=$'\t' read -r n s e u; do
    H_SCORE[$n]=$s; H_ERROR[$n]=$e; H_UNIT[$n]=$u
done < <(extract_primary "$HEAD_JSON")

while IFS=$'\t' read -r n v; do
    B_GC[$n]=$v
done < <(extract_gc "$BASE_JSON")

while IFS=$'\t' read -r n v; do
    H_GC[$n]=$v
done < <(extract_gc "$HEAD_JSON")

mapfile -t ALL_NAMES < <(printf '%s\n' "${!B_SCORE[@]}" "${!H_SCORE[@]}" | sort -u)

# Format "score ± error unit"
fmt_num() {
    awk -v s="$1" -v e="$2" -v u="$3" 'BEGIN { printf "%.2f ± %.2f %s", s, e, u }'
}

# Format percentage change with emoji (lower = better, as JMH averageTime mode)
fmt_change() {
    awk -v b="$1" -v h="$2" 'BEGIN {
        if (b == 0) { print "N/A"; exit }
        pct = (h - b) / b * 100
        if      (pct >  10) emoji = "🔴"
        else if (pct >   5) emoji = "🟡"
        else if (pct <  -5) emoji = "🟢"
        else                emoji = "✅"
        printf "%s %+.1f%%", emoji, pct
    }'
}

# Format a byte count with thousands separators
fmt_bytes() {
    awk -v v="$1" 'BEGIN {
        n = sprintf("%.0f", v)
        len = length(n)
        out = ""
        for (i = 1; i <= len; i++) {
            out = out substr(n, i, 1)
            remaining = len - i
            if (remaining > 0 && remaining % 3 == 0) out = out ","
        }
        print out " B"
    }'
}

printf '## Benchmark Results\n\n'
printf '| Benchmark | Base | PR | Change |\n'
printf '|-----------|:----:|:--:|:------:|\n'

for name in "${ALL_NAMES[@]}"; do
    bs="${B_SCORE[$name]:-}"
    hs="${H_SCORE[$name]:-}"
    if [[ -z $bs ]]; then
        h_str=$(fmt_num "${H_SCORE[$name]}" "${H_ERROR[$name]}" "${H_UNIT[$name]}")
        printf '| `%s` | N/A | %s | 🆕 New |\n' "$name" "$h_str"
    elif [[ -z $hs ]]; then
        b_str=$(fmt_num "${B_SCORE[$name]}" "${B_ERROR[$name]}" "${B_UNIT[$name]}")
        printf '| `%s` | %s | N/A | 🗑️ Removed |\n' "$name" "$b_str"
    else
        b_str=$(fmt_num "$bs" "${B_ERROR[$name]}" "${B_UNIT[$name]}")
        h_str=$(fmt_num "$hs" "${H_ERROR[$name]}" "${H_UNIT[$name]}")
        chg=$(fmt_change "$bs" "$hs")
        printf '| `%s` | %s | %s | %s |\n' "$name" "$b_str" "$h_str" "$chg"
    fi
done

# Memory allocation section (only present when -prof gc is used)
has_mem=false
for name in "${ALL_NAMES[@]}"; do
    bg="${B_GC[$name]:-null}"
    hg="${H_GC[$name]:-null}"
    if [[ $bg != null || $hg != null ]]; then
        has_mem=true
        break
    fi
done

if $has_mem; then
    printf '\n<details>\n<summary>Memory Allocation (bytes/op)</summary>\n\n'
    printf '| Benchmark | Base | PR | Change |\n'
    printf '|-----------|:----:|:--:|:------:|\n'
    for name in "${ALL_NAMES[@]}"; do
        bg="${B_GC[$name]:-null}"
        hg="${H_GC[$name]:-null}"
        if [[ $bg != null && $hg != null ]]; then
            b_str=$(fmt_bytes "$bg")
            h_str=$(fmt_bytes "$hg")
            chg=$(fmt_change "$bg" "$hg")
            printf '| `%s` | %s | %s | %s |\n' "$name" "$b_str" "$h_str" "$chg"
        elif [[ $hg != null ]]; then
            h_str=$(fmt_bytes "$hg")
            printf '| `%s` | N/A | %s | 🆕 New |\n' "$name" "$h_str"
        elif [[ $bg != null ]]; then
            b_str=$(fmt_bytes "$bg")
            printf '| `%s` | %s | N/A | 🗑️ Removed |\n' "$name" "$b_str"
        fi
    done
    printf '\n</details>\n'
fi

printf '\n_Benchmarks run on `ubuntu-latest` with Java 17. Units are ms/op (lower is better)._\n'
