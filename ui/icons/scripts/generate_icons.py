import re
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[3]
SOURCE = REPOSITORY / "icons" / "light" / "animated"
TARGET = REPOSITORY / "ui" / "icons" / "src" / "commonMain" / "kotlin" / "fi" / "kroon" / "vadret" / "icons"
PACKAGE = "se.vadretapp.vadret.icons"
INK = "#4a4f5e"
MASK_CUTOUT = "#000"
GENERATED_DECLARATION = re.compile(r"^val VadretIcons\.(Weather|Symbol)\.", re.M)
GENERATED_FILES = ("IconOutlines.kt", "VadretIconCatalog.kt")

SHARED_OUTLINES = {
    "M251-160q-88": "cloudSilhouette",
    "M253.31-180q": "cloudOutline",
    "m400.69-183.08": "bolt",
}

IGNORED_MOTIONS = {"vdSpin"}

MOTION_KINDS = {
    "vdDrift": "DRIFT",
    "vdFallV": "FALL",
    "vdFallS": "SLANTED_FALL",
    "vdFlakeV": "FLAKE",
    "vdFlakeS": "SLANTED_FLAKE",
    "vdFog": "FOG",
    "vdBolt": "BOLT",
}

EASINGS = {
    "linear": "LINEAR",
    "ease-in-out": "EASE_IN_OUT",
}

INHERITED = ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin")

NUMBER = re.compile(r"[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?")
TOKEN = re.compile(r"[MmLlHhVvCcSsQqTtAaZz]|[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?")

PATH_NODES = {
    "M": ("MoveTo", ("x", "y")),
    "m": ("RelativeMoveTo", ("dx", "dy")),
    "L": ("LineTo", ("x", "y")),
    "l": ("RelativeLineTo", ("dx", "dy")),
    "H": ("HorizontalTo", ("x",)),
    "h": ("RelativeHorizontalTo", ("dx",)),
    "V": ("VerticalTo", ("y",)),
    "v": ("RelativeVerticalTo", ("dy",)),
    "C": ("CurveTo", ("x1", "y1", "x2", "y2", "x3", "y3")),
    "c": ("RelativeCurveTo", ("dx1", "dy1", "dx2", "dy2", "dx3", "dy3")),
    "S": ("ReflectiveCurveTo", ("x1", "y1", "x2", "y2")),
    "s": ("RelativeReflectiveCurveTo", ("dx1", "dy1", "dx2", "dy2")),
    "Q": ("QuadTo", ("x1", "y1", "x2", "y2")),
    "q": ("RelativeQuadTo", ("dx1", "dy1", "dx2", "dy2")),
    "T": ("ReflectiveQuadTo", ("x", "y")),
    "t": ("RelativeReflectiveQuadTo", ("dx", "dy")),
    "A": ("ArcTo", ("horizontalEllipseRadius", "verticalEllipseRadius", "theta", "isMoreThanHalf", "isPositiveArc", "arcStartX", "arcStartY")),
    "a": ("RelativeArcTo", ("horizontalEllipseRadius", "verticalEllipseRadius", "theta", "isMoreThanHalf", "isPositiveArc", "arcStartDx", "arcStartDy")),
}

FLAGS = {"isMoreThanHalf", "isPositiveArc"}


def kotlin_float(value):
    text = ("%.6f" % float(value)).rstrip("0").rstrip(".")
    if text in ("", "-0"):
        text = "0"
    return text + "f"


def camel_case(words):
    return "".join(word[:1].upper() + word[1:] for word in words)


def icon_name(file_stem):
    words = file_stem.replace("halfclear", "half-clear").split("-")
    if words[0].isdigit():
        words = words[1:]
    return camel_case(words)


def lower_first(name):
    return name[:1].lower() + name[1:]


def strip_namespace(tag):
    return tag.split("}", 1)[-1]


def path_nodes(path_data):
    tokens = TOKEN.findall(path_data)
    nodes = []
    index = 0
    command = None
    while index < len(tokens):
        token = tokens[index]
        if not NUMBER.fullmatch(token):
            command = token
            index += 1
            if command in "Zz":
                nodes.append("PathNode.Close")
                continue
        name, parameters = PATH_NODES[command]
        values = tokens[index:index + len(parameters)]
        if len(values) != len(parameters):
            raise ValueError(f"Incomplete {command} in path")
        index += len(parameters)
        arguments = []
        for parameter, value in zip(parameters, values):
            if parameter in FLAGS:
                arguments.append(f"{parameter} = {'true' if float(value) else 'false'}")
            else:
                arguments.append(f"{parameter} = {kotlin_float(value)}")
        nodes.append(f"PathNode.{name}({', '.join(arguments)})")
        if command == "M":
            command = "L"
        elif command == "m":
            command = "l"
    return nodes


def outline_code(path_data, indent):
    padding = " " * indent
    lines = [f"{padding}    {node}," for node in path_nodes(path_data)]
    lines[-1] = lines[-1].rstrip(",")
    return "IconShape.Outline(\n" + f"{padding}nodes = listOf(\n" + "\n".join(lines) + f"\n{padding})\n" + " " * (indent - 4) + ")"


def parse_transform(text):
    translate_x = translate_y = 0.0
    scale = 1.0
    for function, arguments in re.findall(r"(translate|scale)\(([^)]*)\)", text or ""):
        values = [float(value) for value in NUMBER.findall(arguments)]
        if function == "translate":
            translate_x += values[0] * scale
            translate_y += (values[1] if len(values) > 1 else 0.0) * scale
        else:
            scale *= values[0]
    return translate_x, translate_y, scale


def compose_placement(outer, inner):
    outer_x, outer_y, outer_scale = outer
    inner_x, inner_y, inner_scale = inner
    return outer_x + outer_scale * inner_x, outer_y + outer_scale * inner_y, outer_scale * inner_scale


def placement_code(placement):
    translate_x, translate_y, scale = placement
    if (translate_x, translate_y, scale) == (0.0, 0.0, 1.0):
        return None
    return f"IconPlacement(translateX = {kotlin_float(translate_x)}, translateY = {kotlin_float(translate_y)}, scale = {kotlin_float(scale)})"


def seconds_to_milliseconds(text):
    return int(round(float(text.rstrip("s")) * 1000))


def parse_motion(style):
    match = re.search(r"animation:\s*([^;]+)", style or "")
    if not match:
        return None
    name, duration, easing, delay = match.group(1).split()[:4]
    if name in IGNORED_MOTIONS:
        return None
    arguments = [
        f"kind = IconMotionKind.{MOTION_KINDS[name]}",
        f"durationMilliseconds = {seconds_to_milliseconds(duration)}",
        f"delayMilliseconds = {seconds_to_milliseconds(delay)}",
        f"easing = IconEasing.{EASINGS[easing]}",
    ]
    return "IconMotion(" + ", ".join(arguments) + ")"


def stroke_cap(value):
    return {"round": "StrokeCap.Round", "square": "StrokeCap.Square"}.get(value, "StrokeCap.Butt")


def stroke_join(value):
    return {"round": "StrokeJoin.Round", "bevel": "StrokeJoin.Bevel"}.get(value, "StrokeJoin.Miter")


class Generator:

    def __init__(self):
        self.shared_outlines = {}
        self.private_outlines = []

    def outline_reference(self, path_data, icon_property):
        for prefix, name in SHARED_OUTLINES.items():
            if path_data.startswith(prefix):
                self.shared_outlines[name] = path_data
                return name
        name = f"{icon_property}Outline{len(self.private_outlines) + 1}"
        self.private_outlines.append((name, path_data))
        return name

    def element_code(self, node, style, placement, motion, icon_property):
        tag = strip_namespace(node.tag)
        if tag == "path":
            shape = self.outline_reference(node.get("d"), icon_property)
        elif tag == "line":
            shape = (
                f"IconShape.Outline(nodes = listOf(PathNode.MoveTo(x = {kotlin_float(node.get('x1'))}, y = {kotlin_float(node.get('y1'))}), "
                f"PathNode.LineTo(x = {kotlin_float(node.get('x2'))}, y = {kotlin_float(node.get('y2'))})))"
            )
        elif tag == "circle":
            shape = f"IconShape.Circle(centerX = {kotlin_float(node.get('cx'))}, centerY = {kotlin_float(node.get('cy'))}, radius = {kotlin_float(node.get('r'))})"
        else:
            raise ValueError(f"Unsupported element {tag}")
        is_filled = style.get("fill") == INK and tag != "line"
        is_stroked = style.get("stroke") == INK
        if is_filled == is_stroked:
            raise ValueError(f"{tag} must be either filled or stroked with ink")
        if is_filled:
            paint = "IconPaint.Fill"
        else:
            paint = (
                f"IconPaint.Stroke(width = {kotlin_float(style.get('stroke-width', '1'))}, "
                f"cap = {stroke_cap(style.get('stroke-linecap'))}, join = {stroke_join(style.get('stroke-linejoin'))})"
            )
        arguments = [f"shape = {shape}", f"paint = {paint}"]
        placement_argument = placement_code(placement)
        if placement_argument:
            arguments.append(f"placement = {placement_argument}")
        if motion:
            arguments.append(f"motion = {motion}")
        return "IconElement(" + ", ".join(arguments) + ")"

    def collect(self, node, style, placement, motion, icon_property, elements):
        tag = strip_namespace(node.tag)
        child_style = dict(style)
        for attribute in INHERITED:
            if node.get(attribute) is not None:
                child_style[attribute] = node.get(attribute)
        child_placement = compose_placement(placement, parse_transform(node.get("transform")))
        node_motion = parse_motion(node.get("style"))
        if node_motion:
            if motion:
                raise ValueError("Nested motions are not supported")
            if placement != (0.0, 0.0, 1.0):
                raise ValueError("A motion inside a transformed group is not supported")
        child_motion = node_motion or motion
        if tag == "g":
            for child in node:
                self.collect(child, child_style, child_placement, child_motion, icon_property, elements)
        else:
            elements.append(self.element_code(node, child_style, child_placement, child_motion, icon_property))

    def cutouts(self, mask, icon_property):
        codes = []
        for group in mask:
            if strip_namespace(group.tag) != "g":
                continue
            placement = parse_transform(group.get("transform"))
            margin = group.get("stroke-width") if group.get("stroke") == MASK_CUTOUT else None
            for path in group:
                outline = self.outline_reference(path.get("d"), icon_property)
                margin_argument = kotlin_float(margin) if margin else "null"
                placement_argument = placement_code(placement) or "IconPlacement()"
                codes.append(f"IconCutout(shape = {outline}, placement = {placement_argument}, marginWidth = {margin_argument})")
        return codes

    def icon_layers(self, root, icon_property):
        masks = {}
        for definitions in root:
            if strip_namespace(definitions.tag) == "defs":
                for mask in definitions:
                    masks[mask.get("id")] = mask
        layers = []
        plain = []
        for node in root:
            tag = strip_namespace(node.tag)
            if tag in ("defs", "metadata"):
                continue
            mask_reference = re.match(r"url\(#([^)]+)\)", node.get("mask") or "")
            if mask_reference:
                if plain:
                    layers.append(("Plain", None, plain))
                    plain = []
                elements = []
                for child in node:
                    self.collect(child, {}, (0.0, 0.0, 1.0), None, icon_property, elements)
                layers.append(("Masked", self.cutouts(masks[mask_reference.group(1)], icon_property), elements))
            else:
                self.collect(node, {}, (0.0, 0.0, 1.0), None, icon_property, plain)
        if plain:
            layers.append(("Plain", None, plain))
        return layers


def layer_code(kind, cutouts, elements):
    element_lines = ",\n".join(f"                    {element}" for element in elements)
    if kind == "Plain":
        return (
            "            IconLayer.Plain(\n"
            "                elements = listOf(\n"
            f"{element_lines}\n"
            "                )\n"
            "            )"
        )
    cutout_lines = ",\n".join(f"                    {cutout}" for cutout in cutouts)
    return (
        "            IconLayer.Masked(\n"
        "                cutouts = listOf(\n"
        f"{cutout_lines}\n"
        "                ),\n"
        "                elements = listOf(\n"
        f"{element_lines}\n"
        "                )\n"
        "            )"
    )


def icon_file(generator, group, name, layers):
    property_name = lower_first(name)
    private_outlines = "".join(
        f"\nprivate val {outline_name}: IconShape.Outline = {outline_code(path_data, 4)}\n"
        for outline_name, path_data in generator.private_outlines
    )
    non_empty_layers = [layer for layer in layers if layer[2]]
    layers_text = ",\n".join(layer_code(kind, cutouts, elements) for kind, cutouts, elements in non_empty_layers)
    imports = ["androidx.compose.ui.graphics.StrokeCap", "androidx.compose.ui.graphics.StrokeJoin", "androidx.compose.ui.graphics.vector.PathNode"]
    body = "\n".join(f"import {item}" for item in sorted(imports) if item.split(".")[-1] in (layers_text + private_outlines))
    import_block = f"{body}\n\n" if body else ""
    return (
        f"package {PACKAGE}\n\n"
        f"{import_block}"
        f"val VadretIcons.{group}.{name}: VadretIconGraphic\n"
        f"    get() = {property_name}\n"
        f"{private_outlines}\n"
        f"private val {property_name}: VadretIconGraphic by lazy {{\n"
        f"    VadretIconGraphic(\n"
        f"        name = \"{name}\",\n"
        f"        layers = listOf(\n"
        f"{layers_text}\n"
        f"        )\n"
        f"    )\n"
        f"}}"
    )


def shared_outlines_file(shared_outlines):
    declarations = "\n\n".join(
        f"internal val {name}: IconShape.Outline = {outline_code(path_data, 4)}"
        for name, path_data in sorted(shared_outlines.items())
    )
    return f"package {PACKAGE}\n\nimport androidx.compose.ui.graphics.vector.PathNode\n\n{declarations}"


def catalog_file(entries):
    items = ",\n".join(f"    VadretIcons.{group}.{name}" for group, name in entries)
    return f"package {PACKAGE}\n\ninternal val vadretIconCatalog: List<VadretIconGraphic> = listOf(\n{items}\n)"


def main():
    for generated in TARGET.glob("*.kt"):
        if GENERATED_DECLARATION.search(generated.read_text()) or generated.name in GENERATED_FILES:
            generated.unlink()
    shared = {}
    entries = []
    for source in sorted(SOURCE.glob("*.svg")):
        text = re.sub(r"<metadata>.*?</metadata>", "", source.read_text(), flags=re.S)
        root = ElementTree.fromstring(text)
        name = icon_name(source.stem)
        group = "Weather" if source.stem[0].isdigit() else "Symbol"
        generator = Generator()
        layers = generator.icon_layers(root, lower_first(name))
        shared.update(generator.shared_outlines)
        (TARGET / f"{name}.kt").write_text(icon_file(generator, group, name, layers))
        entries.append((group, name))
    (TARGET / "IconOutlines.kt").write_text(shared_outlines_file(shared))
    (TARGET / "VadretIconCatalog.kt").write_text(catalog_file(entries))
    print(f"Generated {len(entries)} icons into {TARGET.relative_to(REPOSITORY)}", file=sys.stderr)


if __name__ == "__main__":
    main()
