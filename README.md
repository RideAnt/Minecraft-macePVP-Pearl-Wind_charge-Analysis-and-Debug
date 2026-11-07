Pearl & Wind Charge Analysis and Debug
This directory contains the client-side main mod and a singleplayer debug companion mod. Both released versions are v1.0.1. The main mod predicts the wind-charge aiming direction from an ender pearl in flight; the Debug mod provides singleplayer debug locking, throw random-offset removal, and logging. By default, both follow the game language and display in Chinese or English.

Installing on 26.2
For Minecraft 26.2 with Fabric Loader 0.19.3 or later, place release/PearlWind_chargeAnalysis-26.2-1.0.1.jar into mods; add release/PearlWind_chargeAnalysisDebug-26.2-1.0.1.jar if you need the debug features. Also install the matching versions of Fabric API and Cloth Config; Mod Menu is used to open the settings. Remove older mods with the same ID before updating, and do not install multiple versions at the same time.

By default, the main mod uses the backslash key to toggle between fixed-point and moving-point mode. Saved key bindings are not overwritten by defaults; if there is a conflict with another mod, you can change it in the Controls settings. The Debug mod's auto-aim and random-offset removal are limited to singleplayer debugging and must be enabled in the settings.

Other versions and merge plan
The source code covers 1.21.4–1.21.11, 26.1–26.1.2, and 26.2.

Use JDK 25. For example, run gradlew.bat build -PtargetVersion=26_2 -PmodVersion=1_0_1 in this directory; add -PdebugCompanion=true for the companion mod. Merged jars for older versions should first be verified per version for references, Mixin behavior, and in-game behavior, and then built with a confirmed version range specified via -PminecraftDependency. Fabric Loader must remain at >=0.19.3.

The trajectory model is an idealized prediction: it accounts for the pearl's discrete motion, its spawn position, inherited player velocity, and the one-tick phase offset, but actual server timing, random spread, hitboxes, entities, and blocks can still affect the result. See 版本变化.txt for the version changes.
