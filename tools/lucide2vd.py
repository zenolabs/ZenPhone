#!/usr/bin/env python3
"""Convert Lucide SVG icons into Android vector drawables.

Lucide draws with strokes rather than fills, and uses shape elements such as
<circle> and <line> that Android does not understand. This script rewrites every
shape as pathData and emits a <vector> with the stroke attributes preserved.

Usage:

    npm install lucide-static
    python tools/lucide2vd.py user phone camera

    python tools/lucide2vd.py --stroke 3 siren
    python tools/lucide2vd.py --color "#1A1A1A" --prefix ic_dark_ lock

Output files are named ic_lucide_<name>.xml, with hyphens turned into
underscores because Android resource names may not contain hyphens.

Lucide is distributed under the ISC License; see the NOTICE file.
"""

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

DEFAULT_ICONS_DIR = os.path.join('node_modules', 'lucide-static', 'icons')
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


def convert(svg_path, stroke, colour):
    root = ET.parse(svg_path).getroot()
    paths = []
    for el in root:
        tag = el.tag.split('}')[-1]
        handler = HANDLERS.get(tag)
        if handler is None:
            raise ValueError('unsupported element <%s> in %s' % (tag, svg_path))
        paths.append(handler(el.attrib))

    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Lucide (https://lucide.dev) - ISC License -->',
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
    p.add_argument('icons', nargs='+', help='Lucide icon names, e.g. user phone camera')
    p.add_argument('--stroke', default='2.5',
                   help='stroke width; Lucide ships 2, ZenPhone uses 2.5 for legibility')
    p.add_argument('--color', default='#FFFFFF', help='stroke colour')
    p.add_argument('--prefix', default='ic_lucide_', help='resource name prefix')
    p.add_argument('--icons-dir', default=DEFAULT_ICONS_DIR)
    p.add_argument('--out-dir', default=DEFAULT_OUT_DIR)
    args = p.parse_args()

    if not os.path.isdir(args.icons_dir):
        sys.exit('Icons directory not found: %s\nRun: npm install lucide-static'
                 % args.icons_dir)
    os.makedirs(args.out_dir, exist_ok=True)

    for name in args.icons:
        svg = os.path.join(args.icons_dir, name + '.svg')
        if not os.path.isfile(svg):
            sys.exit('No such Lucide icon: %s' % name)
        xml = convert(svg, args.stroke, args.color)
        filename = args.prefix + name.replace('-', '_') + '.xml'
        with open(os.path.join(args.out_dir, filename), 'w', encoding='utf-8') as f:
            f.write(xml)
        print('%-34s %d path(s)' % (filename, xml.count('<path')))


if __name__ == '__main__':
    main()
