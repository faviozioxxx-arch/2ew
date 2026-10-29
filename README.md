# NpcPlugin (Paper 1.21.11)

NPC на базе Mannequin: скин по нику, команда по клику.

- `/npc create nickname <ник> [команда]`
- `/npc create command <команда>`
- `/npc list | remove <id> | tp <id> | setcommand <id> <cmd> | setskin <id> <ник> | rename <id> <имя>`

Права: `npc.admin`. В команде можно использовать `{player}`, префикс `console:` — запуск от консоли.

## Сборка
Локально: `gradle build` (Java 21). Jar в `build/libs/`.
GitHub Actions собирает jar при каждом push; при теге `v*` (например `v1.0.0`) создаётся релиз с jar.
