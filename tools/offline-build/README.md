# Offline build

Builds the mod jar without ForgeGradle. See "Building" in the top-level README.

- `build.sh`: the build. Output goes to `build/libs/`; working files to `build/offline/`.
- `stubs/`: compile-only stand-ins for the Minecraft and Forge classes the mod uses, with MCP names and the real signatures. They are never packaged.
- `src/Reobf.java`: renames Minecraft members from MCP to SRG names, walking superclasses and interfaces so inherited members and overrides are renamed too.
- `src/RefCheck.java`: checks the built classes' references against other mod jars.
