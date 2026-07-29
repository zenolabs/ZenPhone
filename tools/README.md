# Tools

## icons2vd.py

Converts stroke-based SVG icons into Android vector drawables.

Written for [Tabler Icons](https://tabler.io/icons) and equally happy with Lucide or any
other set drawn the same way: a 24×24 grid, strokes rather than fills, round caps and joins.
Both sets use shape elements such as `<circle>` and `<line>` that Android's `<vector>` does
not understand, so every shape is rewritten as `pathData` with the stroke attributes kept.
The result stays a true outline icon rather than a flattened silhouette.

From the repository root:

```bash
npm install @tabler/icons
python tools/icons2vd.py user phone camera brand-whatsapp
```

Files land in `app/src/main/res/drawable` as `ic_tabler_<name>.xml`.

### Why Tabler

ZenPhone started on Lucide, which is the same style but carries no brand logos by design.
Tabler is MIT licensed, drawn on the same grid with the same stroke width, and includes some
376 brand icons — so WhatsApp and friends sit in the home grid without a change of style.
Switching sets meant regenerating the drawables and nothing else.

### Stroke width

The sets ship a stroke width of 2 on a 24×24 grid. ZenPhone uses **2.5**, the default here.
The reason is legibility: the app targets people with reduced vision, and thin outlines at
small sizes are noticeably harder to make out. Raise it with `--stroke 3` if an icon still
reads as too faint on a coloured tile.

### Colour

The default stroke colour is `@color/tile_foreground`, which is what the home tiles need:
the tiles keep their own colour in both themes, so an icon following the theme would turn
black in light mode. Where such an icon is reused on a normal background — the settings
list, for instance — the row tints it with `?attr/bald_text_on_button` instead.

### Other options

```bash
python tools/icons2vd.py --color "#1A1A1A" --prefix ic_dark_ lock
python tools/icons2vd.py --icons-dir node_modules/lucide-static/icons --prefix ic_lucide_ user
```

Run `python tools/icons2vd.py --help` for the full list.

### Licensing

Tabler Icons is distributed under the MIT License, which asks only that the copyright notice
be preserved. The full notice lives in the repository's `NOTICE` file and must stay there for
as long as any Tabler-derived drawable ships in the app.

Brand icons are a separate matter: the MIT licence covers the drawing, not the trademark.
Using them to label a button that opens the app in question is ordinary nominative use.
