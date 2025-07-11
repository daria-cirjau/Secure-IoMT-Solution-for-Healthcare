import os
import matplotlib.pyplot as plt
from datetime import datetime, timedelta
import json
import statistics
import matplotlib.dates as mdates

ekg_list = []
temp_list = []
pulse_list = []
oxy_list = []
MAX_POINTS = 100
CODE_MAP = {
    '11524-6': ('EKG', 'red'),
    '8310-5': ('Temperature (°C)', 'orange'),
    '8867-4': ('Pulse (BPM)', 'green'),
    '59408-5': ('Oxygen Saturation (%)', 'blue'),
}


def add_ekg(value):
    ekg_list.append(value)
    if len(ekg_list) > MAX_POINTS:
        ekg_list.pop(0)


def add_temp(value):
    temp_list.append(value)
    if len(temp_list) > MAX_POINTS:
        temp_list.pop(0)


def addPulse(value):
    pulse_list.append(value)
    if len(pulse_list) > MAX_POINTS:
        pulse_list.pop(0)


def addOxygen(value):
    oxy_list.append(value)
    if len(oxy_list) > MAX_POINTS:
        oxy_list.pop(0)


def render():
    try:
        print(f"Render called: ekg={len(ekg_list)}, temp={len(temp_list)}, pulse={len(pulse_list)}, oxy={len(oxy_list)}")
        plt.clf()
        plt.cla()
        plt.close('all')
        fig, axs = plt.subplots(4, 1, figsize=(8, 8))
        # EKG
        if ekg_list:
            axs[0].plot(ekg_list, color='red')
            axs[0].set_title("EKG")
            axs[0].set_xlim(0, MAX_POINTS)
            axs[0].set_ylim(min(ekg_list) - 5, max(ekg_list) + 5)
            axs[0].grid()
        # Temperature
        if temp_list:
            axs[1].plot(temp_list, color='orange')
            axs[1].set_title("Temperature (°C)")
            axs[1].set_xlim(0, MAX_POINTS)
            axs[1].set_ylim(min(temp_list) - 1, max(temp_list) + 1)
            axs[1].grid()
        # Pulse
        if pulse_list:
            axs[2].bar(range(len(pulse_list)), pulse_list, color='green')
            axs[2].set_title("Pulse (BPM)")
            axs[2].set_xlim(0, MAX_POINTS)
            axs[2].set_ylim(min(pulse_list) - 5, max(pulse_list) + 5)
            axs[2].grid()
        # Oxygen
        if oxy_list:
            axs[3].plot(oxy_list, color='blue')
            axs[3].set_title("Oxygen Saturation (SPO2 %)")
            axs[3].set_xlim(0, MAX_POINTS)
            axs[3].set_ylim(min(oxy_list) - 1, max(oxy_list) + 1)
            axs[3].grid()
        plt.tight_layout()
        path = os.path.join(os.environ.get("HOME", "."), "fhir_chart_live.png")
        plt.savefig(path)
        plt.close()
        return path
    except Exception as e:
        print(f"Error in render_live: {e}")
        return ""


def generate_historical_image(obs_json: str, date_after: str, date_before: str) -> str:
    observations = json.loads(obs_json)

    t_after = parse_iso_datetime(date_after).astimezone().replace(tzinfo=None)
    t_before = parse_iso_datetime(date_before).astimezone().replace(tzinfo=None)

    filtered_data = extract_and_filter_observations(observations, t_after, t_before)
    for values in filtered_data.values():
        values.sort(key=lambda x: x[0])

    fig, stats_lines = create_time_series_plot(filtered_data, t_after, t_before)
    render_stats_text(fig, stats_lines)
    return save_figure(fig, t_after, t_before)


def parse_iso_datetime(iso_str: str) -> datetime:
    return datetime.fromisoformat(iso_str.replace('Z', '+00:00'))

def extract_and_filter_observations(data, t_after, t_before):
    history = {code: [] for code in CODE_MAP}

    for entry in data:
        code = entry.get('code', {}).get('coding', [{}])[0].get('code')
        ts = entry.get('effectiveDateTime')
        val = entry.get('valueQuantity', {}).get('value')
        if code in history and ts and val is not None:
            dt = parse_iso_datetime(ts).astimezone().replace(tzinfo=None)
            if t_after <= dt <= t_before:
                history[code].append((dt, float(val)))

    return history


GAP_THRESHOLD = timedelta(minutes=5)
def create_time_series_plot(data, t_after, t_before):
    fig, axs = plt.subplots(4, 1, figsize=(8, 10))
    stats_lines = []

    for ax, (code, (label, color)) in zip(axs, CODE_MAP.items()):
        pts = data.get(code, [])
        times, vals = zip(*pts) if pts else ([], [])

        ax.set_xlim(t_after, t_before)

        gaps = find_gaps(times, t_after, t_before)
        if pts:
            stats_lines.append(format_stats(label, vals))
            plot_segments(ax, times, vals, label, color)
            set_y_limits(ax, vals)
        else:
            stats_lines.append(f"{label.split()[0].lower()}: no data")
            ax.set_ylim(0, 1)

        shade_gaps(ax, gaps)
        format_axis(ax, label)

    plt.tight_layout(rect=[0, 0.15, 1, 1])
    return fig, stats_lines

def find_gaps(times, t_after, t_before):
    if not times:
        return [(t_after, t_before)]

    gaps = []
    if times[0] - t_after > GAP_THRESHOLD:
        gaps.append((t_after, times[0]))

    for prev, curr in zip(times, times[1:]):
        if curr - prev > GAP_THRESHOLD:
            gaps.append((prev, curr))

    if t_before - times[-1] > GAP_THRESHOLD:
        gaps.append((times[-1], t_before))

    return gaps

def format_stats(label, vals):
    return (
        f"{label.split()[0].lower()}: mean {statistics.mean(vals):.1f}, "
        f"min {min(vals):.1f}, max {max(vals):.1f}"
    )

def plot_segments(ax, times, vals, label, color):
    segments = []
    seg_t, seg_v = [times[0]], [vals[0]]

    for prev, t, v in zip(times, times[1:], vals[1:]):
        if t - prev <= GAP_THRESHOLD:
            seg_t.append(t)
            seg_v.append(v)
        else:
            segments.append((seg_t, seg_v))
            seg_t, seg_v = [t], [v]
    segments.append((seg_t, seg_v))

    first = True
    for seg_t, seg_v in segments:
        lbl = label if first else None
        ax.plot(seg_t, seg_v, color=color, label=lbl)
        first = False

def set_y_limits(ax, vals):
    y_min, y_max = min(vals), max(vals)
    pad = (y_max - y_min) * 0.1 if y_max > y_min else y_max * 0.1 or 1
    ax.set_ylim(y_min - pad, y_max + pad)

def shade_gaps(ax, gaps):
    drawn = False
    for start, end in gaps:
        lbl = "No data (gap)" if not drawn else None
        ax.axvspan(start, end, ymin=0, ymax=1,
                   facecolor="fuchsia", alpha=0.2, hatch="//", label=lbl)
        drawn = True

def format_axis(ax, label):
    ax.set_title(label)
    ax.grid(True)
    ax.xaxis.set_major_locator(mdates.AutoDateLocator())
    ax.xaxis.set_major_formatter(mdates.DateFormatter("%H:%M"))
    ax.legend(loc="upper left", bbox_to_anchor=(1.02, 1), borderaxespad=0)


def render_stats_text(fig, stats_lines):
    stats_text = "\n".join(stats_lines)
    fig.text(
        0.5, 0.075, stats_text,
        ha='center', va='center', fontsize=9
    )

def save_figure(fig, t_after, t_before):
    start_str = t_after.strftime('%Y%m%d_%H%M')
    end_str = t_before.strftime('%Y%m%d_%H%M')
    filename = f"fhir_hist_{start_str}_{end_str}.png"
    output_path = os.path.join(os.environ.get('HOME', '.'), filename)

    fig.savefig(output_path, bbox_inches='tight')
    plt.close(fig)
    return output_path
