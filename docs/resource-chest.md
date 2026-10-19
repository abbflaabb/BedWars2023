# Resource chest holograms

Edit `plugins/BedWars2023/config.yml`, save, then run `/bw reload resourcechest`
(permission: the existing reload permission). The command works during a game.
`/bw reload` also refreshes chest settings along with messages.

```yaml
click-in-chest-to-deposit-feature:
  enable: true
  blocked-items:
    - WOOD_SWORD
    - SHEARS
    - COMPASS
resource-chest:
  hologram:
    enabled: true
    x-offset: 0.5
    y-offset: 0.04
    z-offset: 0.5
    spacing: 0.25
    title: STORAGE CHEST
    subtitle: '&7Left-click to deposit'
```

X/Z offsets are measured from the block's minimum corner: 0.5 centers the text.
Y offset now positions the lowest text line above the top of the chest block,
compensating for the invisible armor stand's name height. Increase it to raise
both lines; decrease it to lower them. Spacing controls the gap between lines.
Existing `y-offset: 1.5` values are preserved: change to `0.04` for the lower
placement. No values in your existing configuration are silently overwritten.

Reload replaces existing holograms in playing arenas and applies text, colors,
offsets, spacing, enable switches, and blocked items. Invalid numeric settings
or material names reject the reload before changing the active configuration.
Disable removes existing holograms; enable recreates them in current games.

Install the rebuilt JAR with the server stopped and start it once. Subsequent
configuration changes only need the command above. Client rendering should be
checked in-game; automated checks exercise positioning and reload lifecycle
without a live Minecraft client.
