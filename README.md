# Pearl-Wind_charge-Analysis-and-Debug

Source repository for the client-side Fabric projects `PearlWind_chargeAnalysis` and its optional singleplayer testing companion, `PearlWind_chargeAnalysisDeBug`.

## Select a source bundle

Choose the top-level folder that exactly matches the Minecraft version and Fabric Loader version you intend to build. Each bundle contains two independent Gradle projects:

- `PearlWind_chargeAnalysis`: the main HUD and trajectory-analysis mod.
- `PearlWind_chargeAnalysisDeBug`: the optional companion. It requires the main mod for the same Minecraft version, but does not pin a specific main-mod iteration.

## Requirements

- Fabric API and Cloth Config API are required. Their minimum supported versions are declared in each project's `fabric.mod.json`.
- Mod Menu is optional and provides the in-game configuration screens.
- Minecraft 1.21.11 projects target Java 21. Minecraft 26.x projects target Java 25.

Each subproject includes its own MIT `LICENSE`, build instructions, changelog, and release-specific README.
