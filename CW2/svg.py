from pathlib import Path
from xml.etree import ElementTree as ET

svg_path = Path("/Users/XXXXRT/Desktop/UoE/SEPP/CourseWork/CW2/Task2.svg")
root = ET.parse(svg_path).getroot()

colors = ["#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA"]
i = 0

for el in root.iter():
    if not el.tag.endswith("path"):
        continue
    cls = el.get("class", "")
    if "edge-pattern-dashed" in cls or "edge-pattern-dotted" in cls:
        c = colors[i % len(colors)]
        i += 1
        el.set("stroke", c)
        style = el.get("style", "")
        parts = [p.strip() for p in style.split(";") if p.strip() and not p.lower().startswith("stroke:")]
        parts.append(f"stroke: {c}")
        el.set("style", "; ".join(parts))

ET.ElementTree(root).write(svg_path, encoding="utf-8", xml_declaration=False)
