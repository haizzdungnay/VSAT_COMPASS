---
name: V-SAT Compass Operational Admin
colors:
  surface: '#fcf8ff'
  surface-dim: '#dad7f3'
  surface-bright: '#fcf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f2ff'
  surface-container: '#efecff'
  surface-container-high: '#e8e5ff'
  surface-container-highest: '#e2e0fc'
  on-surface: '#1a1a2e'
  on-surface-variant: '#474553'
  inverse-surface: '#2f2e43'
  inverse-on-surface: '#f2efff'
  outline: '#787585'
  outline-variant: '#c8c4d6'
  surface-tint: '#594ac9'
  primary: '#321ba3'
  on-primary: '#ffffff'
  primary-container: '#4a3aba'
  on-primary-container: '#c2bbff'
  inverse-primary: '#c6bfff'
  secondary: '#5644d0'
  on-secondary: '#ffffff'
  secondary-container: '#6f5fea'
  on-secondary-container: '#fffbff'
  tertiary: '#342989'
  on-tertiary: '#ffffff'
  tertiary-container: '#4b42a1'
  on-tertiary-container: '#c1bbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e4dfff'
  primary-fixed-dim: '#c6bfff'
  on-primary-fixed: '#160066'
  on-primary-fixed-variant: '#402eb0'
  secondary-fixed: '#e4dfff'
  secondary-fixed-dim: '#c6bfff'
  on-secondary-fixed: '#160066'
  on-secondary-fixed-variant: '#4029ba'
  tertiary-fixed: '#e4dfff'
  tertiary-fixed-dim: '#c6c0ff'
  on-tertiary-fixed: '#150066'
  on-tertiary-fixed-variant: '#423897'
  background: '#fcf8ff'
  on-background: '#1a1a2e'
  surface-variant: '#e2e0fc'
typography:
  headline-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Be Vietnam Pro
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Be Vietnam Pro
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Be Vietnam Pro
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Be Vietnam Pro
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.5rem
  margin: 1.5rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

The design system is constructed for mission-critical enterprise EdTech administration, supporting exam authors, academic reviewers, subject matter experts, and system operators. The tone is authoritative, rigorous, and highly functional. It avoids frivolous decorative patterns in favor of structured data density, immediate scanning, and zero-latency operational efficiency.

Drawing from modern enterprise Material 3 foundations, the aesthetic couples crisp architectural structure with deep indigo and purple accents. This balances academic prestige with contemporary cloud-software ergonomics. Visual complexity is managed through explicit typographic contrast, structured surface boundaries, and calibrated status indicators engineered for continuous, intensive use.

## Colors

The palette establishes a disciplined chromatic hierarchy tailored for long working sessions without visual fatigue.

### Core Swatches
- **Primary (`#4A3ABA`)**: Primary navigation, active table row selections, dominant call-to-action buttons, key metric badges.
- **Primary Dark (`#352A8A`)**: Deep contrast states, high-priority hovered controls, fixed master sidebar background.
- **Primary Light (`#7C6BD9`)**: Focus halos, selected container tints (`rgba(124, 107, 217, 0.08)`), subtle iconography.
- **Accent (`#6C5CE7`)**: Interactive highlights, progress tracking anchors, live review cursors.
- **Neutral Primary (`#1A1A2E`)**: Body copy, high-contrast headings, numeric inputs, data cell values.
- **Neutral Secondary (`#757575`)**: Metadata, contextual help, inactive labels, input placeholders, table headers.
- **Surface (`#FFFFFF`)**: Card surfaces, modal layers, editable canvas elements, table backgrounds.
- **Background (`#F5F5FA`)**: Base viewport, canvas underlays, inactive layout regions.

### Operational Semantics
- **Success (`#27AE60`)**: Verified answer states, finalized publications, valid score distributions.
- **Error (`#D32F2F`)**: Validation failures, point calculation imbalances, destructive actions.
- **Warning (`#F39C12`)**: Draft status, pending approvals, incomplete grading keys.
- **Information (`#2196F3`)**: Collaborator notices, operational audit logs, real-time sync status.

### Exam Section & Item Type Identifiers
- **True / False (9 items / 54 pts)**: Distinct Cyan-Indigo identity (`#0288D1`, container `rgba(2, 136, 209, 0.08)`).
- **Single Choice (6 items / 36 pts)**: Indigo identity (`#4A3ABA`, container `rgba(74, 58, 186, 0.08)`).
- **Matching (5 items / 30 pts)**: Amber-Orange identity (`#E65100`, container `rgba(230, 81, 0, 0.08)`).
- **Short Answer (5 items / 30 pts)**: Emerald-Teal identity (`#00796B`, container `rgba(0, 121, 107, 0.08)`).
- **Total Calibration Indicator**: Dedicated indicator for the 25 questions / 150 points standard ledger.

## Typography

The type scale is standardized on **Be Vietnam Pro** across headlines, labels, and running text. It provides native support for Vietnamese tone marks and diacritics without vertical alignment shifts or font-fallback stutter. 

- **Display & Headings**: Reserved for top-level operational dashboards, section titles, and modal headers. Tighter letter spacing on `headline-lg` maintains balance across large Vietnamese compound nouns.
- **Body Text**: Tuned for question stems, rationales, grading rubrics, and academic explanations. `body-md` is the administrative standard for list views, data panels, and metadata blocks.
- **Labels & Numbers**: Used across exam metrics, point ledger chips, table column headers, and status indicators. Numerical figures use tabular lining figures (`font-variant-numeric: tabular-nums`) to ensure vertical alignment across points and scoring grids.

## Layout & Spacing

The portal uses a high-density, fluid-responsive administrative layout structured around a fixed 260px administrative navigation spine and a dynamic workspace canvas.

### Layout Mechanics
- **Grid Architecture**: 12-column fluid grid for primary data displays. The gutter width is standard at `1rem` (16px), switching to `gutter-compact` (`0.5rem` / 8px) within nested property inspectors, score-breakdown panels, and formula sidebars.
- **Section Margins**: Outer canvas padding is pinned to `margin` (`1.5rem` / 24px) on screens larger than 1024px, condensing to `margin-mobile` (`1rem` / 16px) on compact tablets or secondary monitor splits.
- **Information Density Rhythm**: Component vertical padding adheres to strict intervals. Form inputs and table rows use standard heights: 36px for compact administrative tables and 44px for primary form fields.

### Form Factor Behavior
- **Desktop (>= 1280px)**: 3-column split view support: Left navigation spine, central authoring canvas, right telemetry/scoring inspector (320px fixed).
- **Tablet / Small Laptop (1024px - 1279px)**: Scoring inspector folds into a docked collapsible drawer. Primary 12-column data views span full canvas width.
- **Mobile (< 768px)**: Read-only supervision view; global navigation collapses to an off-canvas drawer, questions stack vertically, and tables switch to horizontal-scroll compact ledgers.

## Elevation & Depth

Visual hierarchy uses tonal surface layering combined with low-contrast structural borders, rather than deep ambient shadows. This prevents interface fatigue during prolonged authoring workflows.

- **Level 0 (Canvas Surface)**: Default app background (`#F5F5FA`), recessed, flat, zero elevation.
- **Level 1 (Card & Section Containers)**: White surface (`#FFFFFF`) with a 1px perimeter border in `#E4E4EE`. Used for question items, data grid containers, and toolbar ribbons.
- **Level 2 (Dropdowns, Filters, Floating Palettes)**: Surface `#FFFFFF`, border `#D8D8E5`, shadow `0 4px 12px rgba(26, 26, 46, 0.06), 0 1px 2px rgba(26, 26, 46, 0.04)`.
- **Level 3 (Modals, Question Preview Overlays, Critical Dialogs)**: Surface `#FFFFFF`, shadow `0 12px 32px rgba(26, 26, 46, 0.12), 0 2px 6px rgba(26, 26, 46, 0.08)`. Backdrops use `rgba(26, 26, 46, 0.45)` with a 2px backdrop blur.
- **Interactive Focus States**: No ambient elevation jumps; interactive components receive an active outline: `0 0 0 3px rgba(74, 58, 186, 0.18)` coupled with border color `#4A3ABA`.

## Shapes

The interface employs a disciplined **Soft** shape profile (`roundedness: 1`):
- **Base Components (Inputs, Buttons, Badges, Table Rows)**: `0.25rem` (4px). Provides a clean, functional architectural feel suitable for tables, inline tools, and data-dense layouts.
- **Medium Panels (Cards, Modals, Section Blocks)**: `0.5rem` (8px). Delivers subtle visual grouping without wasting canvas area.
- **Pill Exceptions**: Reserved strictly for read-only status indicators, question-type tokens, and numerical point ledgers (`rounded: 9999px`) to separate metadata badges from actionable buttons and controls.

## Components

### Buttons
- **Primary**: Solid background `#4A3ABA`, text `#FFFFFF`, border-radius 4px, height 36px (compact) or 40px (default), font `label-md`. Hover: `#352A8A`. Active: inset shadow.
- **Secondary / Outlined**: Border 1px solid `#4A3ABA`, text `#4A3ABA`, background transparent. Hover: background `rgba(74, 58, 186, 0.04)`.
- **Tertiary / Ghost**: Text `#1A1A2E`, background transparent. Hover: `#F0F0F7`.
- **Destructive**: Border or fill using `#D32F2F`, text `#FFFFFF` (fill) or `#D32F2F` (outline).

### Chips & Badges
- **Status Badges**: Semi-transparent backgrounds with high-contrast text.
  - *Draft*: Background `rgba(243, 156, 18, 0.12)`, text `#B7791F`.
  - *Published*: Background `rgba(39, 174, 96, 0.12)`, text `#1E8449`.
  - *Reviewing*: Background `rgba(33, 150, 243, 0.12)`, text `#1565C0`.
- **Exam Question Type Badges**:
  - *True / False*: Pill shape, border 1px solid `rgba(2, 136, 209, 0.3)`, background `rgba(2, 136, 209, 0.08)`, text `#0288D1`, format: `Đúng/Sai · 9 câu (54 điểm)`.
  - *Single Choice*: Border 1px solid `rgba(74, 58, 186, 0.3)`, background `rgba(74, 58, 186, 0.08)`, text `#4A3ABA`, format: `Trắc nghiệm · 6 câu (36 điểm)`.
  - *Matching*: Border 1px solid `rgba(230, 81, 0, 0.3)`, background `rgba(230, 81, 0, 0.08)`, text `#E65100`, format: `Ghép đôi · 5 câu (30 điểm)`.
  - *Short Answer*: Border 1px solid `rgba(0, 121, 107, 0.3)`, background `rgba(0, 121, 107, 0.08)`, text `#00796B`, format: `Trả lời ngắn · 5 câu (30 điểm)`.
- **Score Ledger Balance Token**: Global indicator positioned in the header ribbon displaying `Tổng: 25 câu / 150 điểm`. Emits a semantic green indicator when criteria match exactly, and pulsing amber/red when imbalanced.

### Input Fields & Controls
- **Text & Numerical Inputs**: Height 36px, background `#FFFFFF`, border 1px solid `#D8D8E5`, radius 4px, font `body-md`. Focus: border `#4A3ABA`, ring `0 0 0 2px rgba(74, 58, 186, 0.15)`. Error: border `#D32F2F`.
- **Checkboxes & Radio Controls**: 16px square/circle, border 1.5px solid `#757575`. Active/Checked state: solid `#4A3ABA` fill with `#FFFFFF` checkmark or center pip. Focus ring conforms to primary halo.

### Administrative Data Tables
- **Header Row**: Background `#F8F8FD`, height 40px, text `#757575`, font `label-sm`, uppercase, letter-spacing `0.04em`. Bottom border 1px solid `#E4E4EE`.
- **Body Rows**: Height 44px, alternating hover state `#F9F9FC`, selected state background `rgba(74, 58, 186, 0.05)`, bottom border 1px solid `#F0F0F6`. 
- **Inline Operations**: Context action triggers stay concealed until row hover to preserve visual clarity across dense datasets.

### Question Authoring Cards
- **Container**: White background, 1px solid border `#E4E4EE`, 8px corner radius.
- **Card Header**: Displays question index (`Câu 01`), points allotment badge (`6.0 điểm`), item type token, and quick reorder handles.
- **Stem Area**: Integrated rich text/LaTeX container with light gray inset canvas (`#FAFAFC`) and subtle left accent bar matching the question type's semantic color.