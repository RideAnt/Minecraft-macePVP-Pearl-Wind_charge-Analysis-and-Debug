# PearlWind Test Build v1

**Release name:** `MC 1.21.11 - Fabric Loader 0.19.2 - Test Build v1`  
**Internal artifact version:** `1.0.1`  
**Author:** RideAnt and Codex
**Tip:** these mod are AI generated

## Shared dependencies

- Fabric API
- Cloth Config API
- Mod Menu

## Main mod

File: `PearlWind_chargeAnalysis-1.0.1.jar`

- Provides fixed-point and moving-point three-dimensional trajectory analysis.
- Tracks P1/P2 and renders compact HUD information and visual guides.
- Supports independent-axis and perspective-projection guide modes.
- Provides configurable angle boundaries, timing compensation, and optional path validation.
- Does not include automatic camera locking or projectile-spread modification.
- Does not require the Debug companion.

## Debug companion

File: `PearlWind_chargeAnalysisDeBug-1.0.1.jar`

- Requires PearlWind_chargeAnalysis.
- Does not pin a specific main-mod iteration version.
- Provides singleplayer-only P1/P2 camera locking.
- Provides deterministic projectile-spread testing on the integrated server.
- Provides optional diagnostic logging.
- Keeps every Debug feature disabled by default.
- Cannot be installed together with the old PearlWind_ProjectileAccuracyTest mod.

## Installation

For normal gameplay, install only JAR files that do not contain `sources` in their names. Source JAR files are provided for code inspection and development tools and should not be placed in the game's `mods` folder.

The main mod can run without the Debug companion. The Debug companion cannot run without the main mod.

## Scope

The main mod calculates and displays predicted trajectories; it does not alter vanilla collision, teleportation, or explosion behavior. The Debug companion's automatic camera locking and projectile-spread testing are restricted to singleplayer in the official build.
