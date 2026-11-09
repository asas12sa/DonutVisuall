# DonutVisual (rein clientseitig, Fabric 1.21.11)

RShift -> Hub-Menue:
- Regeln: beliebiges Item -> beliebiges Item (Modell, Name, Lore, Sell-Preis)
- Presets: Spawner (Skeleton, Blaze, Piglin, Spider, Iron Golem ...), Elytra-Optik, Netherite-Gear
- Schematics: .litematic / .schem aus config/donutvisual/schematics/ als Ghost-Basis

Fake-Spawner: Rechtsklick mit leerer Hand = lokales Menue, Rechtsklick mit dem Quell-Item = Stack +1.
Shift+Rechtsklick = normal platzieren. Alles nur visuell.

Bauen: Gradle-Wrapper aus https://github.com/FabricMC/fabric-example-mod kopieren, Versionen in
gradle.properties mit https://fabricmc.net/develop abgleichen, dann `./gradlew build`.
