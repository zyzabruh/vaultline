# Vaultline

A [Meteor Client](https://meteorclient.com) addon for Minecraft 1.21.11: **44 modules** in a single `Stash` category,
two GUI themes and the ClickGUI of the client zio.

The addon grew around stash organisation and now also carries a travel and flight suite, base hunting, a set of
rendering and ambient effects, and a GUI that can be switched to the copied client's look.

Author: **pro0101**

## Modules

### Stash and storage

- `stash-organizer` — opens every nearby storage block and reorganises the stash in place (see below).
- `shulker-sorter` — sorts storage by category, with blocks registered by tag key.
- `auto-dye-shulkers` — dyes shulker boxes and bundles standing in crafting grids.
- `stash-brander` — renames items in bulk at anvils.
- `better-stash-finder` — Meteor's stash finder with more settings.
- `base-finder` — scans chunk packets for stashes and bases, with block lists and entity detectors.
- `search-area` — loads chunks over a rectangle, or spirals outward, for stash finding and map saving.
- `flow-esp` — marks unusual lava and water flows that reveal player bases.
- `chest-tracker` and the `chesttracker` command (aliases `ct`, `track`) — remembers every container you open, indexes
  its contents per dimension and searches that index.
- `sign-history` — remembers every sign, including destroyed and changed ones, and can write their text back.
- `easy-access` — opens hidden containers within interaction range.
- `fast-portal` — builds a portal in the direction you are looking.
- `storage-esp`, `item-esp` — boxes containers and colours dropped items through walls.
- `overview` — shows the most common item over shulkers and bundles.
- `fast-frame` — fills placed item frames with items, which is what the stash labelling leaves behind.

### Travel and flight

- `navigation` — interactive map navigation through the nether.
- `auto-pilot` — travels toward the navigation goal.
- `elytra-tweaks` — tweaks and automation for elytra flight.
- `bounce-fly` — elytra bouncing for fast highway travel.
- `control-fly` — controlled flight with automatic rocket boosting, porpoise and a no-durability chest swap.
- `air-place` — places blocks in the air with vanilla timing.
- `mining-tweaks` — queued packet mining with double break support.
- `rocket-cycle`, `rocket-fly`, `rocket-boost`, `elytra-recast`, `pitch-40` — the flight chain: a height band or key
  following with rockets, level flight with timed fireworks and obstacle lookahead, a Grim-aware boost engine, glide
  recast and the pitch-40 mode that drives them.
- `elytra-mace` — glides to the nearest target, gains height, dives, swaps to a chestplate, lands the mace hit and puts
  the elytra back on.
- `trail-follower`, `chunk-radar` — follows trails in every dimension, and follows the 1.12 trail from arriving chunks.
- `hotbar-refill`, `inventory-cleaner` — tops up hotbar stacks before they run out, and throws out what a keep-list does
  not name.

### Rendering and ambience

- `shaders` — outlines players, mobs and storage entities through walls, with a glow, a rim and a fill.
- `block-shader` — boxes the blocks you list, with animated shader colours.
- `skybox` — replaces the sky with a fullscreen shader.
- `weather`, `ambience`, `world-particles` — drawn weather, fog colours per dimension and world particles.
- `trails`, `jump-circles`, `hit-effect`, `totem-animation`, `animations` — movement trails, jump rings, hit markers,
  the totem pop and the hand swing.
- `two-d-esp`, `name-tags`, `render-optimizer` — flat ESP, custom name tags, and no fire overlay or hurt camera.

### GUI

Two themes sit next to Meteor's own: `Futuristic` (cyan and violet, flat corners, gradient title bars) and `Zio`, which
opens the copied client's ClickGUI. That screen draws its own panels — 99 pixels wide, 14 pixel title bar, 13 pixel
rows, the `+` / `-` indicator on the right, settings unfolded inside the panel on a right click — with that client's
colours, its title-bar fade and its row gradient. Panels drag by their title bar, fold on a right click and scroll per
panel, booleans fill, numbers drag and enum values cycle. GUI sounds are Meteor's own; the client's audio files and
font atlas are not bundled.

The port is recorded effect by effect in [`ZIO-GUI-PARITY.md`](ZIO-GUI-PARITY.md): every animation, blur, halo,
gradient and transition of that client's GUI, with the test that proves it or the evidence for leaving it out.

## Stash Organizer

The module the addon started from. It opens every nearby storage block and reorganises the stash in place: items are
grouped, chests are labelled with item frames, and shulker boxes are unpacked, packed or kept shut.

- Scans chests, barrels and shulker boxes in a configurable radius and reads their contents.
- Groups items by exact item, vanilla family, broad category or metal/gem material, and creates equipment kits.
- Reuses the existing containers as the chests of those groups, and places extra chests (or overflow shulkers) when a
  group has no room. When the stash runs out of chests, the groups that have to share one are packed by kind: redstone
  with redstone, blocks with blocks, ores with ores, instead of a bit of everything in every shared chest.
- Reads the item frame already standing on a chest and gives that group to the chest first, so a stash labelled by hand
  keeps its layout.
- Unpacks shulker boxes found in the stash, packs overflow boxes with a pickaxe, and collects boxes left on the floor.
- Never puts a shulker box inside another one.
- Keeps renamed boxes, bulk boxes, kits and boxes holding a protected item shut; the end-of-run report lists them with
  the rule that protected them.
- Labels every sorted chest with an item frame holding a representative item.
- Tidies the slots of every chest before labelling: the stacks of one item sit together, the groups keep the order of
  the plan and the empty slots end at the bottom of the chest (setting `arrange-chests`).
- Empties a chest that holds items of other groups when the buffer has nowhere to go, so a chest is not left half full
  while its own group waits elsewhere.
- Writes a plan that survives a disconnection, a report of the run and the group-to-chest layout of the previous runs.
  The report has a row per chest: stacks received, stacks given away, stacks taken out to make room, stacks still out of
  place and the reason they stayed.
- Walks with Baritone when it is installed, and falls back to Meteor's path manager otherwise.

## Install

1. Install Meteor Client for the matching Minecraft version.
2. Drop the built jar in the `mods` folder next to Meteor.
3. Join a world and open the Meteor GUI: the addon's modules are in the `Stash` category, its themes in the theme
   picker.

## Usage

1. Stand in the storage room, near the chests to organise, with at least 8 free inventory slots (or enable the inventory
   dump).
2. Enable `Stash Organizer`. It scans, plans, places the chests it needs, sorts, arranges, labels and writes its report.
3. The run stops on its own. The plan, the report and the remembered layout of the previous runs are written in the
   Meteor folder, under `vaultline`.
4. If the connection drops mid-run, joining the same world re-enables the module and resumes the saved plan.

## Requirements

`trail-follower` and `chunk-radar` need [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap),
[Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) and
[XaeroPlus](https://modrinth.com/mod/xaeroplus) installed. Without them the addon still loads: those two modules are
simply not registered and the rest of the addon works normally.

Baritone is optional: `stash-organizer` walks with it when it is present.

## Build

```bash
./gradlew build
```

The jar lands in `build/libs`.

## Imported modules

Most of the travel, hunt, render and storage modules come from other Meteor addons and are distributed here under the
GPL-3.0, with their authors credited: the Hyperglide travel suite (`fast-frame`, `overview`, `navigation`,
`auto-pilot`, `elytra-tweaks`, `bounce-fly`, `control-fly`, `air-place`, `mining-tweaks`, `fast-portal`,
`easy-access`), `shulker-sorter`, `sign-history`, `trail-follower`, `chunk-radar`, `pitch-40`, `elytra-recast`,
`rocket-boost`, `hotbar-refill` and `inventory-cleaner` from HunterBuddy, and `auto-dye-shulkers`, `chest-tracker`,
`stash-brander`, `better-stash-finder` and `search-area` from BepHax. `CREDITS.md` lists the repositories, commits and
the changes made.

`control-fly` adds the `porpoise` climb/dive swing with `porpoise-height`, an `alignment` control that can leave the
yaw to Baritone, and a `chest-swap` that delegates no-durability flight to `elytra-tweaks`' spoof mode.

`elytra-mace` glides to the nearest target in `range`, gains `height` above it, dives at `dive-pitch`, swaps the elytra
for a chestplate (never one with Curse of Binding), lands the mace hit and puts the elytra back on for the climb out.
While it is diving, `control-fly` and `elytra-recast` stand down so they do not fight it for the steering.

`chest-tracker` remembers every chest, barrel, shulker box, hopper and dispenser you open, indexes their contents per
dimension, and searches that index from the `Y` browser or the `chesttracker` command. It can also open nearby
containers itself and draw the saved containers in the world.

The rest is original work for this addon: `stash-organizer` and its planner, sorting, labelling, report and
chest-placement code, `base-finder`, `flow-esp`, the `skybox`, `shaders`, `block-shader`, `weather`, `ambience`,
particle, trail and animation modules, the `Futuristic` and `Zio` themes and the ported ClickGUI.

Two HUD elements live in the `Stash` group: `stash-organizer`, and `flight-status`, which lists the running flight
modules with the `pitch-40` bounds and the `trail-follower` mode so that chain is not invisible.

## License

GNU General Public License v3.0 or later, see `LICENSE`. Modules imported from other addons keep their original
authorship; `CREDITS.md` lists them.
