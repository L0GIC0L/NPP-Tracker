#!/usr/bin/env python3
"""Regenerate app/src/main/assets/reactors.json from the NRC website.

Static unit details (docket, type, licensed MWt, license dates, ...) do not
change often, so they are bundled with the app. Run this script when the NRC
adds or removes a unit, then commit the updated JSON.

    python3 tools/generate_reactors.py
"""
import html
import json
import re
import time
import urllib.request
from pathlib import Path

BASE = "https://www.nrc.gov"
LIST_URL = BASE + "/facilities-safety/operating-reactors/list-power-reactor-units"
STATUS_URL = (BASE + "/documents-reports/document-collections/events-reports-associated-with/"
              "power-reactor-status-reports/PowerReactorStatusForLast365Days.txt")
OUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/reactors.json"

# The daily status report and the reactor list spell a few units differently.
STATUS_NAME_ALIASES = {
    "Shearon Harris 1": "Harris 1",
    "La Salle 1": "LaSalle 1",
    "La Salle 2": "LaSalle 2",
    "River Bend 1": "River Bend Station 1",
}


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "NPP-Tracker metadata generator"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode("utf-8", errors="replace")


def text(fragment):
    fragment = re.sub(r"<br\s*/?>", " / ", fragment)
    fragment = re.sub(r"<[^>]+>", "", fragment)
    return re.sub(r"\s+", " ", html.unescape(fragment)).strip()


DATE = r"\d{1,2}/\d{1,2}/\d{4}"


def info_lines(page):
    """Return the "Label: value" lines of a plant page's facility info block."""
    start = page.find("<strong>Location")
    end = page.find("Plant Diagram", start)
    block = page[start:end if end > 0 else start + 5000]
    return [text(line) for line in re.split(r"<br\s*/?>|</p>", block) if text(line)]


def find_line(lines, *labels):
    for line in lines:
        if any(line.lower().startswith(label.lower()) for label in labels):
            return line
    return None


def after_colon(line):
    return line.split(":", 1)[1].strip() if line and ":" in line else None


def dates(line):
    return re.findall(DATE, line or "")


def parse_plant_page(page, fallback_name):
    lines = info_lines(page)
    title = re.search(r'<span class="usa-current">(.*?)</span>', page, flags=re.S)
    issued = dates(find_line(lines, "Operating License", "Combined License:"))
    renewed = dates(find_line(lines, "Renewed"))
    expires_line = next((l for l in lines if "Expires" in l), None)
    if expires_line and "Expires" in expires_line:
        expires_line = expires_line[expires_line.index("Expires"):]
    expires = dates(expires_line)
    power = re.search(r"([\d,]+(?:\.\d+)?)", after_colon(find_line(lines, "Licensed MWt", "Licensed Reactor Power")) or "")
    return {
        "fullName": text(title.group(1)) if title else fallback_name,
        "licensedMwt": round(float(power.group(1).replace(",", ""))) if power else None,
        "licenseIssued": issued[0] if issued else None,
        "licenseRenewed": renewed[0] if renewed else None,
        "licenseExpires": expires[0] if expires else None,
        "vendor": after_colon(find_line(lines, "Reactor Vendor")),
        "containment": after_colon(find_line(lines, "Containment Type")),
    }


def main():
    status_names = {line.split("|")[1] for line in fetch(STATUS_URL).splitlines()[1:] if "|" in line}
    listing = fetch(LIST_URL)
    units = []
    for row in re.findall(r"<tr.*?</tr>", listing, flags=re.S)[1:]:
        cells = re.findall(r"<td[^>]*>(.*?)</td>", row, flags=re.S)
        if len(cells) < 6:
            continue
        href = re.search(r'href="([^"]+)"', cells[0])
        name, docket = [p.strip() for p in text(cells[0]).split(" / ")]
        location = text(cells[3])
        unit = {
            "name": STATUS_NAME_ALIASES.get(name, name),
            "displayName": name,
            "docket": docket,
            "license": text(cells[1]),
            "type": text(cells[2]),
            "location": location,
            "state": location.rsplit(",", 1)[-1].strip(),
            "operator": text(cells[4]),
            "region": int(text(cells[5])),
            "nrcUrl": BASE + href.group(1) if href else None,
        }
        if unit["nrcUrl"]:
            unit.update(parse_plant_page(fetch(unit["nrcUrl"]), name))
            time.sleep(0.5)
        units.append(unit)
        print(f"{unit['name']:<28} {unit.get('licensedMwt')} MWt  {unit.get('licenseIssued')} "
              f"{unit.get('licenseRenewed')} {unit.get('licenseExpires')}  {unit.get('vendor')}")

    missing = status_names - {u["name"] for u in units}
    if missing:
        print("WARNING: units in the status report without metadata:", sorted(missing))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(sorted(units, key=lambda u: u["name"]), indent=1) + "\n")
    print(f"Wrote {len(units)} units to {OUT}")


if __name__ == "__main__":
    main()
