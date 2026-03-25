# DailyPlaytimeCap

### NOTE: Fabric API is required
A server-side Minecraft mod that lets administrators set daily playtime limits for players.

## Features
- Set a player's daily playtime limit
- Check remaining time
- Remove a limit
- Automatically kicks players when their time runs out (they can't relog for the day)
- Add extra time for a player for the day

## Commands
- `/dailyplaytime set <player> <minutes>`
- `/dailyplaytime check [player]`
- `/dailyplaytime remove <player>`
- `/dailyplaytime add_extra_time <player> <minutes>`