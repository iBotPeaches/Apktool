#!/usr/bin/env python3
"""
Compare two JMH benchmark result JSON files and output a Markdown summary.

Usage:
    python3 compare-benchmarks.py <base.json> <head.json>
"""

import json
import sys


def load_benchmarks(path):
    """Load a JMH JSON results file and return a dict keyed by benchmark name."""
    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    results = {}
    for bench in data:
        # Use the simple method name (last segment after the last dot).
        name = bench["benchmark"].split(".")[-1]
        primary = bench["primaryMetric"]
        results[name] = {
            "score": primary["score"],
            "error": primary["scoreError"],
            "unit": primary["scoreUnit"],
            "secondary": bench.get("secondaryMetrics", {}),
        }
    return results


def format_change(base_score, head_score):
    """Return (percentage_string, emoji) comparing head vs base."""
    if base_score == 0:
        return "N/A", ""
    change = (head_score - base_score) / base_score * 100
    # For average time (lower = better): increases are regressions.
    if change > 10:
        emoji = "🔴"
    elif change > 5:
        emoji = "🟡"
    elif change < -10:
        emoji = "🟢"
    elif change < -5:
        emoji = "🟢"
    else:
        emoji = "✅"
    return f"{change:+.1f}%", emoji


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <base.json> <head.json>", file=sys.stderr)
        sys.exit(1)

    base_results = load_benchmarks(sys.argv[1])
    head_results = load_benchmarks(sys.argv[2])
    all_names = sorted(set(list(base_results.keys()) + list(head_results.keys())))

    print("## Benchmark Results\n")
    print("| Benchmark | Base | PR | Change |")
    print("|-----------|:----:|:--:|:------:|")

    for name in all_names:
        base = base_results.get(name)
        head = head_results.get(name)

        if base is None:
            head_str = f"{head['score']:.2f} ± {head['error']:.2f} {head['unit']}"
            print(f"| `{name}` | N/A | {head_str} | 🆕 New |")
        elif head is None:
            base_str = f"{base['score']:.2f} ± {base['error']:.2f} {base['unit']}"
            print(f"| `{name}` | {base_str} | N/A | 🗑️ Removed |")
        else:
            unit = base["unit"]
            base_str = f"{base['score']:.2f} ± {base['error']:.2f} {unit}"
            head_str = f"{head['score']:.2f} ± {head['error']:.2f} {unit}"
            change, emoji = format_change(base["score"], head["score"])
            print(f"| `{name}` | {base_str} | {head_str} | {emoji} {change} |")

    # Memory allocation section (populated when -prof gc is used).
    # The middle dot (·) is JMH's metric namespace separator in secondary metric names.
    mem_key = "·gc.alloc.rate.norm"
    base_mem_data = {
        name: r["secondary"].get(mem_key, {}).get("score")
        for name, r in base_results.items()
    }
    head_mem_data = {
        name: r["secondary"].get(mem_key, {}).get("score")
        for name, r in head_results.items()
    }
    has_memory = any(
        v is not None
        for v in list(base_mem_data.values()) + list(head_mem_data.values())
    )

    if has_memory:
        print()
        print("<details>")
        print("<summary>Memory Allocation (bytes/op)</summary>\n")
        print("| Benchmark | Base | PR | Change |")
        print("|-----------|:----:|:--:|:------:|")

        for name in all_names:
            base_mem = base_mem_data.get(name)
            head_mem = head_mem_data.get(name)

            if base_mem is not None and head_mem is not None:
                change, emoji = format_change(base_mem, head_mem)
                print(
                    f"| `{name}` | {base_mem:,.0f} B | {head_mem:,.0f} B"
                    f" | {emoji} {change} |"
                )
            elif head_mem is not None:
                print(f"| `{name}` | N/A | {head_mem:,.0f} B | 🆕 New |")
            elif base_mem is not None:
                print(f"| `{name}` | {base_mem:,.0f} B | N/A | 🗑️ Removed |")

        print()
        print("</details>")

    print()
    print(
        "_Benchmarks run on `ubuntu-latest` with Java 17. "
        "Units are ms/op (lower is better)._"
    )


if __name__ == "__main__":
    main()
