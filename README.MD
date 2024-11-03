# Eclipse

**Eclipse** is a Paper plugin that introduces SpongePowered/Fabric Mixin to the Paper server environment. This project implements a modified version of the Ignite Mixin Launcher, modified to be more flexible and agnostic in terms of the Minecraft version(allowing it to function on numerous versions with little issues) and built specifically for the Eclipse environment.

Eclipse includes a large array of features:
- **Access Wideners**
- **Fabric Mixins**
- **Mixin Extras**

## Getting Started

To use Eclipse, include the following options in your `paper-plugin.yaml` (or `.yml`):

```yaml
mixins: [
  "example.mixins.json"
]
wideners: [
  "eclipse.accesswidener"
]
datapack-entry: true
```

### Configuration Fields

- **mixins**: Defines the Mixin configuration files your plugin will use

  Example:
  ```json
  {
    "mixins": [
      "ExampleMixin"
    ],
    "package": "me.dueris.example.mixin",
    "compatibilityLevel": "JAVA_21"
  }
  ```

- **wideners**: Specifies the access wideners.

- **datapack-entry**: Allows Eclipse to also load your plugin as a vanilla datapack, similar to a fabric mod.

## Installation
1. Download Eclipse from [modrinth](https://modrinth.com/plugin/eclipse-mixin).
2. Place the Eclipse plugin jar in your servers `plugins` directory.
3. Configure your plugins `paper-plugin.yaml` as shown above.

## Examples
For a plugin named `ExamplePlugin`:
- `paper-plugin.yaml`
  ```yaml
  name: ExamplePlugin
  main: me.dueris.example.Main
  version: 1.0.0
  mixins: [
    "eclipse.mixins.json"
  ]
  wideners: [
    "eclipse.accesswidener"
  ]
  datapack-entry: true
  ```

- `eclipse.mixins.json`
  ```json
  {
    "mixins": [
      "ExampleMixin"
    ],
    "package": "me.dueris.example.mixin",
    "compatibilityLevel": "JAVA_21"
  }
  ```

## License
This project is licensed under GPL-3.0 with an additional permission clause:
Redistribution and use in binary form are allowed, provided that explicit permission is obtained from the author for direct integration into third-party projects.

See the [LICENSE](https://github.com/Dueris/Eclipse/blob/master/LICENSE) file for more details.

---