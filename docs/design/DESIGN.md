# Design System: Tactical Brutalism

## 1. Overview & Creative North Star
### Creative North Star: "The Kinetic Console"
This design system rejects the "softness" of modern SaaS. It is a high-fidelity digital manifestation of a cold, high-tech control room. The goal is to move beyond mere dashboarding into the realm of **Tactical Brutalism**. We are not building a website; we are building an instrument panel for high-stakes decision-making.

The aesthetic is driven by functional density, pixel-perfect alignment, and an atmosphere of restrained power. We break the "standard" layout by utilizing an ultra-rigid grid that feels like it was etched into a slate-blue obsidian surface. Intentional asymmetry is achieved through "Information Clusters"—dense pockets of monospaced data surrounded by expansive, cold negative space.

## 2. Colors & Surface Logic
The palette is rooted in a "Deep Space" hierarchy, where darkness is the substrate and light is reserved exclusively for critical information.

### The "No-Line" Rule (Reinterpreted)
While the user requested bordered containers, we interpret this through a **High-End Editorial** lens. Traditional 1px solid borders are replaced by **Tonal Recessions**. Sectioning is defined by shifting between `surface-container-lowest` and `surface-container-low`. Where a line is required to mimic an industrial panel, use the `outline-variant` at 20% opacity—creating a "ghost border" that suggests structure without cluttering the optical field.

### Surface Hierarchy & Nesting
*   **Base Layer (`surface` / `background`):** The obsidian bedrock. Used for the primary environment.
*   **Primary Panels (`surface-container-low`):** Large structural sections of the dashboard.
*   **Nested Modules (`surface-container-high`):** For active data modules or focused equipment readouts.
*   **The "Glass & Gradient" Rule:** Floating overlays (modals or tooltips) must utilize `surface-bright` with a 12px `backdrop-filter: blur()`. To inject "soul," CTAs use a linear gradient from `primary` (#FFB693) to `primary_container` (#FF6B00) at a 45-degree angle, mimicking the glow of a physical backlit button.

## 3. Typography: The Logic of Information
Our typography system is a dual-engine setup designed for split-second legibility and technical authority.

*   **The Technical Engine (Space Grotesk / Monospaced):** Used for all `display` and `headline` tokens. This creates a "CRT-style" terminal vibe. For critical data, use `display-lg` with a subtle `text-shadow` (0 0 8px primary) to simulate phosphorus bloom.
*   **The Operational Engine (Inter):** Used for `body` and `label` tokens. Inter provides the clean, sans-serif contrast needed to make technical labels readable at small scales (`label-sm` at 0.6875rem).
*   **Hierarchy as Identity:** Large-scale headlines (`headline-lg`) are always uppercase and tracked out (letter-spacing: 0.1em) to command authority, while body text remains tight and functional.

## 4. Elevation & Depth: Tonal Layering
In this system, "Up" does not mean "Shadow." It means "Glow" or "Contrast."

*   **The Layering Principle:** Avoid drop shadows for structural elements. Instead, use "Step-Up" coloring. If a card sits on `surface-container-low`, the card itself should be `surface-container-high`.
*   **Ambient Shadows:** For floating status HUDs, use an ultra-diffused shadow: `box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4)`. The shadow color must be derived from `surface-container-lowest` to maintain the "cold" atmosphere.
*   **CRT Scan-Line Texture:** Apply a global CSS overlay using a repeating linear gradient (transparent 50%, rgba(18, 20, 22, 0.05) 50%) with a background-size of 100% 4px. This anchors the UI in the "equipment panel" aesthetic.

## 5. Components

### Buttons (Tactical Actuators)
*   **Primary:** Solid `primary_container` (#FF6B00). Hard 0px corners. Hover state includes a "glowing" outer box-shadow in `primary`.
*   **Secondary:** Ghost variant. `outline` border (20% opacity) with `on_surface` text. 
*   **Tertiary:** All-caps `label-md` with a leading `+` or `_` character to emphasize the brutalist, coded nature of the UI.

### Data Chips & Status Indicators
*   **Selection Chips:** Use `secondary_container` (Cyan) for active states. Text must be `on_secondary_container` for maximum contrast.
*   **Hazard Indicators:** Use `tertiary` (Hazardous Yellow) for warnings. These components should "pulse" using a subtle opacity animation (0.8 to 1.0) to draw attention without being jarring.

### Input Fields
*   **Style:** Recessed. Use `surface-container-lowest` for the background. 
*   **Focus State:** The border transitions from `outline-variant` to a 1px solid `secondary` (Cyan) with a subtle neon glow. No rounded corners.

### Cards & Lists
*   **Constraint:** No 1px dividers. Separate list items using 8px of vertical `surface-container-low` spacing.
*   **Interaction:** On hover, a card’s background shifts to `surface-bright`.

### Specialty Component: The "Data Grid"
A bespoke component for industrial monitoring. This is a dense matrix of `label-sm` data points. Vertical alignment is enforced by a 1px `outline-variant` (10% opacity) vertical rule every 4 columns, mimicking an engineering ledger.

## 6. Do’s and Don’ts

### Do:
*   **Align to the Pixel:** If an element is off by 1px, the "industrial" illusion breaks.
*   **Use Monospacing for Numbers:** Always use `Space Grotesk` or a monospace fallback for changing numerical values to prevent "jumping" text.
*   **Embrace High Contrast:** Use `safety orange` and `glowing cyan` sparingly but boldly to guide the eye to critical failures or successes.

### Don't:
*   **Don't Round Corners:** All `borderRadius` tokens are set to `0px`. Roundness suggests consumer-grade softness; we require industrial-grade rigidity.
*   **Don't Use Generic Greys:** Always use the provided `slate blues` and `charcoal` tokens. Pure grey (#808080) is forbidden as it flattens the "Obsidian" depth.
*   **Don't Over-Animate:** Transitions should be "Instant" (0ms) or "Mechanical" (Step-easing). Avoid "bouncy" or "organic" easing functions.