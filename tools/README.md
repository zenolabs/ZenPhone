# Tools

## lucide2vd.py

Converts [Lucide](https://lucide.dev) SVG icons into Android vector drawables.

Lucide draws with strokes rather than fills, and uses shape elements such as
`<circle>` and `<line>` that Android's `<vector>` does not understand. The script
rewrites every shape as `pathData` and keeps the stroke attributes, so the result
stays a true outline icon rather than a flattened silhouette.

From the repository root:

```bash
npm install lucide-static
python tools/lucide2vd.py user phone camera
```

Files land in `app/src/main/res/drawable` as `ic_lucide_<name>.xml`.

### Stroke width

Lucide ships a stroke width of 2 on a 24×24 grid. ZenPhone uses **2.5**, which is
the default here. The reason is legibility: the app targets people with reduced
vision, and thin outlines at small sizes are noticeably harder to make out than
the filled glyphs they replaced. Raise it further with `--stroke 3` if an icon
still reads as too faint on a coloured tile.

### Other options

```bash
python tools/lucide2vd.py --color "#1A1A1A" --prefix ic_dark_ lock
python tools/lucide2vd.py --out-dir app/src/main/res/drawable-night bell
```

Run `python tools/lucide2vd.py --help` for the full list.

### Licensing

Lucide is distributed under the ISC License. The full notice lives in the
repository's `NOTICE` file and must stay there for as long as any Lucide-derived
drawable ships in the app.

Lucide contains no brand logos by design, so icons such as WhatsApp keep their
original artwork.
