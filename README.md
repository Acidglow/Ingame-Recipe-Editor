# Acidglow's Ingame Recipe Editor

A NeoForge mod for Minecraft 26.2 that lets permitted players inspect and edit recipes without leaving the game.

Open the editor with:

```
/recipeeditor
```

## Features

- Browse registered items in the Item Book, including item-tag information.
- Preview crafting-table, furnace, blast-furnace, and campfire recipes.
- Navigate items with multiple recipes and view tag-based ingredients.
- Create new custom crafting and cooking recipes from the editor grid.
- Remove individual recipes and restore their original default recipes later.
- Choose a specific deleted recipe from the Removed Recipes list before restoring it.
- Hide items from recipes and compatible JEI views, then reveal them again when needed.
- Restore every changed recipe with `/recipeeditor restore_all`.

## Permissions

By default, the editor is available only to Creative-mode players or operators with permission level 2 or higher. Both settings can be changed in the mod configuration.

Recipe changes, removed defaults, custom recipes, and hidden-item settings are saved with the world and are reapplied when it is loaded.

## Requirements

- Minecraft 26.2
- NeoForge 26.2
