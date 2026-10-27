---
name: "source-command-design"
description: "Use when creating or editing Android layouts, drawables, themes, or colors in VSAT Compass — to apply the correct design tokens and component patterns instead of guessing values."
---

# source-command-design

Use this skill when the user asks to run the migrated source command `design`.

## Command Template

# VSAT Compass Design System

Read `DESIGN.md` at the project root. It contains the authoritative YAML design tokens and all usage rules.

## How to use

**Before writing any layout or drawable XML:**

1. Check `DESIGN.md` YAML front matter for the exact hex/dp value you need
2. Use `@color/` references — never hardcode hex in layouts
3. Use existing drawables from the `Drawables Reference` table before creating new ones

## Quick lookup

| Need | DESIGN.md section |
|------|-------------------|
| Color hex values | `colors.*` |
| Padding/margin dp | `spacing.*` |
| Corner radius dp | `radii.*` |
| Which drawable to use | `## Drawables Reference` |
| Button/chip/card spec | `components.*` |
| Font size | `typography.scale.*` |
| Dark mode rules | `## Dark Mode` |

## Key rules (from DESIGN.md)

- **Semantic colors are exclusive** — green = correct/success only, orange = warning/bookmark only, never swap
- **Spacing in multiples of 4dp** — use md(16), xl(24), xxl(32) for consistency
- **State-based drawables exist** — for answer options use `bg_option_selected/default`, for question grid use `bg_question_answered/current/unanswered`
- **Buttons have hierarchy** — primary > outline > outline_white (for dark backgrounds)
- **Always use `@color/` attributes** — never hardcode hex in layout XML

## Adding new design elements

If a needed token or component does not exist in `DESIGN.md`:
1. Propose the new token following existing naming conventions
2. Add to `res/values/colors.xml` (and night variant if needed)
3. Update `DESIGN.md` YAML and the relevant prose section
