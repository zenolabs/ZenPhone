#!/usr/bin/env python3
"""Convert stroke-based SVG icons into Android vector drawables.

Written for Tabler Icons, and equally happy with Lucide or any other set drawn the same
way: a 24x24 grid, strokes rather than fills, round caps and joins. Both sets use shape
elements such as <circle> and <line> that Android's <vector> does not understand, so every
shape is rewritten as pathData with the stroke attributes preserved.

Usage:

    npm install @tabler/icons
    python tools/icons2vd.py user phone camera brand-whatsapp

    python tools/icons2vd.py --stroke 3 sos
    python tools/icons2vd.py --icons-dir node_modules/lucide-static/icons --prefix ic_lucide_ user

Output files are named <prefix><name>.xml, with hyphens turned into underscores because
Android resource names may not contain hyphens.

Tabler Icons is distributed under the MIT License; see the NOTICE file.
"""

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

DEFAULT_ICONS_DIR = os.path.join('node_modules', '@tabler', 'icons', 'icons', 'outline')
DEFAULT_OUT_DIR = os.path.join('app', 'src', 'main', 'res', 'drawable')


def circle_to_path(a):
    cx, cy, r = float(a['cx']), float(a['cy']), float(a['r'])
    return 'M%g,%g a%g,%g 0 1,0 %g,0 a%g,%g 0 1,0 %g,0' % (
        cx - r, cy, r, r, 2 * r, r, r, -2 * r)


def ellipse_to_path(a):
    b = dict(a)
    b['r'] = a['rx']
    return circle_to_path(b)


def rect_to_path(a):
    x, y = float(a.get('x', 0)), float(a.get('y', 0))
    w, h = float(a['width']), float(a['height'])
    r = float(a.get('rx', a.get('ry', 0)) or 0)
    if r <= 0:
        return 'M%g,%g h%g v%g h%g z' % (x, y, w, h, -w)
    return ('M%g,%g h%g a%g,%g 0 0 1 %g,%g v%g a%g,%g 0 0 1 %g,%g h%g '
            'a%g,%g 0 0 1 %g,%g v%g a%g,%g 0 0 1 %g,%g z') % (
        x + r, y, w - 2 * r, r, r, r, r, h - 2 * r, r, r, -r, r,
        -(w - 2 * r), r, r, -r, -r, -(h - 2 * r), r, r, r, -r)


def line_to_path(a):
    return 'M%g,%g L%g,%g' % (
        float(a['x1']), float(a['y1']), float(a['x2']), float(a['y2']))


def points_to_path(a, close):
    pts = re.findall(r'-?[\d.]+', a['points'])
    if len(pts) < 4:
        raise ValueError('polyline with fewer than two points')
    body = ' '.join('L%s,%s' % (pts[i], pts[i + 1]) for i in range(2, len(pts), 2))
    return 'M%s,%s %s%s' % (pts[0], pts[1], body, ' z' if close else '')


HANDLERS = {
    'path': lambda a: a['d'],
    'circle': circle_to_path,
    'ellipse': ellipse_to_path,
    'rect': rect_to_path,
    'line': line_to_path,
    'polyline': lambda a: points_to_path(a, False),
    'polygon': lambda a: points_to_path(a, True),
}


def is_bounding_box(attrib):
    """Tabler opens every icon with an invisible 24x24 path used only for spacing.

    It carries neither stroke nor fill, so it draws nothing in the browser - but copied
    into a vector drawable and given a stroke colour it would suddenly appear as a square
    around the glyph.
    """
    return attrib.get('stroke') == 'none' and attrib.get('fill') == 'none'


def convert(svg_path, stroke, colour):
    root = ET.parse(svg_path).getroot()
    paths = []
    for el in root:
        tag = el.tag.split('}')[-1]
        if is_bounding_box(el.attrib):
            continue
        handler = HANDLERS.get(tag)
        if handler is None:
            raise ValueError('unsupported element <%s> in %s' % (tag, svg_path))
        paths.append(handler(el.attrib))

    if not paths:
        raise ValueError('nothing to draw in %s' % svg_path)

    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Tabler Icons (https://tabler.io/icons) - MIT License -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="24dp"',
           '    android:height="24dp"',
           '    android:viewportWidth="24"',
           '    android:viewportHeight="24">',
           '']
    for d in paths:
        out += ['    <path',
                '        android:fillColor="#00000000"',
                '        android:pathData="%s"' % d.replace('"', ''),
                '        android:strokeColor="%s"' % colour,
                '        android:strokeLineCap="round"',
                '        android:strokeLineJoin="round"',
                '        android:strokeWidth="%s" />' % stroke]
    out += ['</vector>', '']
    return '\n'.join(out)


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('icons', nargs='+', help='icon names, e.g. user phone brand-whatsapp')
    p.add_argument('--stroke', default='2.5',
                   help='stroke width; the sets ship 2, ZenPhone uses 2.5 for legibility')
    p.add_argument('--color', default='@color/tile_foreground', help='stroke colour')
    p.add_argument('--prefix', default='ic_tabler_', help='resource name prefix')
    p.add_argument('--icons-dir', default=DEFAULT_ICONS_DIR)
    p.add_argument('--out-dir', default=DEFAULT_OUT_DIR)
    args = p.parse_args()

    if not os.path.isdir(args.icons_dir):
        sys.exit('Icons directory not found: %s\nRun: npm install @tabler/icons'
                 % args.icons_dir)
    os.makedirs(args.out_dir, exist_ok=True)

    for name in args.icons:
        svg = os.path.join(args.icons_dir, name + '.svg')
        if not os.path.isfile(svg):
            sys.exit('No such icon: %s' % name)
        xml = convert(svg, args.stroke, args.color)
        filename = args.prefix + name.replace('-', '_') + '.xml'
        with open(os.path.join(args.out_dir, filename), 'w', encoding='utf-8') as f:
            f.write(xml)
        print('%-34s %d path(s)' % (filename, xml.count('<path')))


if __name__ == '__main__':
    main()
