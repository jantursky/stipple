# Stipple design

The gallery and the docs share one look: a white page, one violet accent and a lot of dither. The dithers themselves
are black on white (or the reverse); the page around them stays quiet so they carry the colour and the texture.

## Palette

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| page | `#ffffff` | `#09090b` | Page background |
| panel | `#fafafa` | `#111113` | Cards, controls, the settings panel |
| hairline | `#e4e4e7` | `#27272a` | 1 px borders and dividers |
| text | `#09090b` | `#fafafa` | Headings, body |
| muted | `#52525c` | `#a1a1aa` | Secondary text |
| faint | `#71717a` | `#8e8e98` | Captions, labels |
| accent | `#615fff` | `#8b8aff` | Focus, the active item, small marks. One accent only. |
| code | `#f4f4f5` | `#18181b` | Code panels |

The dark values are derived: surfaces step up from near-black in small increments, and the accent is lightened so it
keeps contrast on a dark page.

## Type

Geist for text and headings (weight 500 to 600, no black weights), Geist Mono for numbers, code and the frame meter.
Both are SIL OFL fonts, bundled with the gallery and loaded from `@fontsource-variable` in the docs.

## Shape

Radii 6 (controls), 12 (cards and panels) and 16 (large stages). Borders are 1 px hairlines. No shadows: depth comes
from the dither itself, as in `DitherButton`.

## Logo

A 16 by 16 grid of square cells in a Bayer 8x8 diagonal ramp, from paper at the top left to ink at the bottom right.
It is monochrome (ink on paper, inverted in dark mode) and keeps crisp edges at every size. Launcher icons stretch the
grid to the edges of the icon. Source files are in `design/brand`.

## Rules

- Do: let the dither be the only decoration, keep cells whole pixels, keep the accent for focus and selection.
- Do: check every screen in light and dark, and with the cell size at both extremes.
- Don't: add gradients, shadows or a second accent.
- Don't: put text on a dither whose tone is above about 0.5.
