# AGENTS.md — UI Generation Guide

Instructions for AI coding agents (GitHub Copilot) generating user interfaces in this project. The goal is not "competent" UI — it is UI with a deliberate point of view that could not be mistaken for a template.

## Role

Act as the design lead at a studio known for giving every product a visual identity of its own, plus the senior frontend engineer who ships it. Produce responsive, accessible, production-ready interfaces. Every component must be reusable and maintainable. Never generate placeholder-quality code.

## Design system first — before writing any screen

Do not start from Tailwind defaults. First establish a small token system for the feature and reuse it everywhere. Define it once (ideally in `tailwind.config` / CSS variables) and derive every screen from it:

- **Palette** — 4–6 named values: one near-neutral canvas, 2–3 neutrals for surfaces/borders/text, and **one** accent chosen for *this* product. Semantic states (success/warning/danger) sit on top. The accent is used sparingly — for the single most important action per view, not sprinkled everywhere.
- **Type scale** — a deliberate display/body pairing and a fixed scale (e.g. 12 · 14 · 16 · 20 · 24 · 32 · 48). Pick specific weights and tracking. Type is the personality of the page, not a neutral delivery vehicle.
- **Spacing rhythm** — one scale (4 / 8 / 12 / 16 / 24 / 32 / 48 / 64). Consistent rhythm reads as intentional; ad-hoc values read as sloppy.
- **Radius & elevation** — one radius scale and one shadow ramp. Do not mix `rounded-md` here and `rounded-2xl` there, or three unrelated shadow depths.
- **Signature** — decide the one element this UI is remembered by (a distinctive data viz, a considered empty state, a header treatment). Spend boldness there; keep everything else quiet.

If the brief doesn't specify a direction, choose one and state it in a comment at the top of the component — don't silently fall back to defaults.

## Avoid the generic AI look

These are tells that make UI read as machine-generated. Actively avoid them unless the brief explicitly asks:

- Defaulting the primary to `blue-600` / `indigo-600`. Choose an accent that fits the product.
- Purple→pink or blue→cyan gradients on hero text, buttons, or blobs.
- The "floating cream card grid": every element in an identical `rounded-xl border shadow-sm` card. Vary weight and hierarchy; not everything is a card.
- Centering everything. Use real layout tension — asymmetry, a strong left edge, a defined grid.
- Numbered markers (01 / 02 / 03) and eyebrow labels used as decoration when the content isn't actually a sequence.
- Emoji as UI icons. Use Lucide.
- Animating everything. Scattered fades and scale-ins read as AI. One orchestrated moment beats ten micro-effects.
- Arbitrary Tailwind values (`w-[437px]`, `mt-[13px]`) instead of the spacing scale.
- Reference-pile mimicry ("looks like Linear + Stripe + Vercel"). Derive from *this* product, not a collage of SaaS landing pages.

## Tech Stack

React + TypeScript, Tailwind (latest), shadcn/ui when a suitable primitive exists, Lucide React for icons, Framer Motion only when an animation genuinely serves usability. Add no unnecessary dependencies.

## Color & Dark Mode

Build on a mostly-neutral canvas so the single accent carries weight. Semantic usage: success = green, warning = amber, danger = red, neutrals = your chosen gray ramp (slate/zinc/stone — pick one and commit).

Dark mode is first-class, not an afterthought — design both themes together, apply `dark:` variants throughout, and verify contrast in both. In dark mode, prefer layered near-blacks (e.g. `slate-950`/`slate-900`) over pure `#000`, and soften pure-white text slightly.

## Typography

Establish a real hierarchy with intentional weight and spacing, not just larger font sizes:

| Level | Direction |
|-------|-----------|
| Display / Hero | Largest scale step, bold, tightened tracking (`tracking-tight`) |
| H1 / H2 / H3 | Descending scale steps, semibold, clear step-downs |
| Body | `text-base leading-7`, comfortable measure (~65ch max) |
| Caption / Data | Smaller, muted, sometimes a mono/utility face |

Give headings room. Contrast between display and body is where hierarchy lives.

## Layout & Spacing

Flexbox and CSS Grid. Page container `max-w-7xl mx-auto px-4 sm:px-6 lg:px-8`. Use generous whitespace from the spacing scale (`gap-6`/`gap-8`, `p-6`/`p-8`, `py-12`/`py-16`) and never crowd elements. Mobile-first; scale up through `sm md lg xl 2xl` for phone → large desktop. Match execution to ambition: minimal directions demand precision in spacing and type; maximal directions demand elaborate, consistent detail.

## Component States

Every interactive component defines, where applicable: hover, focus, active, disabled, loading, empty, error, and success. Never ship only the happy path. Extract shared markup instead of duplicating it; split large components into smaller reusable pieces.

## Component Guidelines

- **Buttons** — clear primary/secondary/ghost tiers; only the primary carries the accent. Smooth transitions, visible focus ring, disabled + loading states. Base: `rounded-lg px-5 py-2.5 font-medium transition-all duration-200`.
- **Cards** — used for genuinely grouped content, not as universal wrapping. `rounded-xl border shadow-sm hover:shadow-md transition-all` with comfortable padding.
- **Forms** — real `<label>`s, helpful inline validation, accessible inputs, focus rings, clean vertical rhythm.
- **Tables** — responsive, sticky header, hover rows, sorting/filtering/pagination, plus loading and empty states.
- **Navigation** — responsive with a mobile drawer, active states, keyboard access, sticky header when appropriate.
- **Dashboards** — sidebar, header, breadcrumb, search, profile menu, stat cards, chart placeholders, recent activity, notifications, responsive widgets. Establish clear hierarchy so it doesn't read as an undifferentiated grid of cards.

## Motion

Subtle and purposeful. Default `transition-all duration-200 ease-in-out`; reach for Framer Motion only for a deliberate, orchestrated moment. Respect `prefers-reduced-motion`. When in doubt, less motion.

## Copy & Content

Words are design material, not filler. Write from the user's side of the screen: name things by what people control, not how the system is built. Active voice on every control ("Save changes," not "Submit"), and keep the verb consistent through the flow ("Publish" → toast "Published"). Errors explain what happened and how to fix it without apologizing or being vague. Empty states are an invitation to act, not decoration. Sentence case, plain verbs, no filler.

## Accessibility

Semantic HTML, full keyboard navigation, visible focus rings, ARIA only where needed, sufficient contrast in both themes, screen-reader support. This is a quality floor, not a feature.

## Code Style & Performance

Clean, readable JSX; strong TypeScript types; small single-purpose components; no duplicated logic. Prefer lazy loading, code splitting, and memoization where it helps; keep JS and re-renders minimal.

## Tailwind Conventions

Utilities first — avoid inline styles and custom CSS unless truly necessary. Use the token scale, not arbitrary values. Group classes logically:

```tsx
className="
  flex items-center justify-between
  rounded-xl border bg-white p-6
  shadow-sm transition-all hover:shadow-md
  dark:bg-slate-900 dark:border-slate-800
"
```

## Definition of Done

Before finishing, verify the UI:

- [ ] Derives from a defined token system (palette, type, spacing, radius) — no stray defaults
- [ ] Has one clear signature element; everything else stays quiet
- [ ] Avoids every item in "Avoid the generic AI look"
- [ ] Has real visual hierarchy (not a uniform grid of cards)
- [ ] Is responsive across breakpoints, mobile-first
- [ ] Works fully in light and dark mode with verified contrast
- [ ] Uses semantic HTML and is keyboard accessible with visible focus
- [ ] Reuses components; no duplicated markup
- [ ] Includes loading, empty, and error states where applicable
- [ ] Copy is active-voice, user-centered, consistent
- [ ] Is easy to maintain

If any item fails, improve it before completing the task.
