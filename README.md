# TickRate
The source code for [TickRate](https://modrinth.com/mod/tick)

## API
From TickRate v0.3.0 onwards, an API is exposed to allow other mods to programmatically use the `/tick` command. The API consists of 2 parts:
1. `TickRateAPI` - Basically the `/tick` command in API form. Obtain the API instance via `TickRateAPI.getInstance()`.
2. `TickRateEvents` - Events that fire when a server/entity/chunk's tick rate/status is modified. Registering event handlers is identical to Fabric's events.

To use the API in your mod, first add the [Modrinth Maven repository](https://support.modrinth.com/en/articles/8801191-modrinth-maven#h_fac44e6b48). 
Then, add the dependency in this format:

```groovy
dependencies {
    // e.g. modImplementation "maven.modrinth:tick:0.3.0-1.21.4"
    modImplementation "maven.modrinth:tick:${mod_version}-${mc_version}"
}
```

The API's documentation is provided by the sources/javadoc JAR hosted on the same repository. Due to an issue with Modrinth Maven, you may need to download these JARs manually.

## Some cool datapacks
These datapacks require the TickRate mod to function.

_This is being released here because I can't be bothered to create another Modrinth project just for this :P_

1. EntityRandomTickRate (TickRate <a download href="https://github.com/DennisOchulor/TickRate/raw/refs/heads/main/datapacks/EntityRandomTickRate 0.1.x.zip">0.1.x</a> / <a download href="https://github.com/DennisOchulor/TickRate/raw/refs/heads/main/datapacks/EntityRandomTickRate 0.2.x.zip">0.2.x+</a>)

   A datapack that randomises the tick rate of every entity (except players).

2. ChunkRandomTickRate (TickRate <a download href="https://github.com/DennisOchulor/TickRate/raw/refs/heads/main/datapacks/ChunkRandomTickRate 0.1.x.zip">0.1.x</a> / <a download href="https://github.com/DennisOchulor/TickRate/raw/refs/heads/main/datapacks/ChunkRandomTickRate 0.2.x.zip">0.2.x+</a>)

   A datapack that randomises the tick rate of every chunk.