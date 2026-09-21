# Acidglow's Ingame Recipe Editor

A server-authoritative NeoForge mod for Minecraft 26.2. Permitted players can inspect, create, remove, restore, and hide recipes without leaving the game. Recipe changes update the live recipe index immediately, are stored with the world, and are reapplied whenever recipes reload.

## Version branches

- `26.2.X` targets Minecraft 26.2.
- `26.1.X` contains the 26.1 backport. Use the JAR built for the exact Minecraft and NeoForge patch that you run.

## Requirements

- Minecraft 26.2
- NeoForge 26.2.0.84 or newer

## Installation

Install the same mod JAR and compatible NeoForge version on the server and every client that will open the editor. Put the JAR in each instance's `mods` directory, then start the game or server once to generate configuration files.

The common configuration is named `acidglows_ingame_recipe_editor-common.toml`. NeoForge stores it in the instance `config` directory and may copy it to a world's `serverconfig` directory. It controls editor access; Item Book inventory grants always require Creative mode or an operator at permission level 2 or higher.

## Recommended companion mod: Polymorph+

[Polymorph+](https://modrinth.com/mod/polymorph_plus) is recommended when custom crafting recipes use the same ingredients as another recipe. It lets players choose the intended result. Install the matching NeoForge release on both server and clients.

## Opening the editor

Run:

```
/recipeeditor
```

By default, only Creative-mode players and operators at permission level 2 or higher can use it. The common configuration can change editor access and its required operator level.

## What the editor supports

- Browse registered items in the Item Book, grouped by creative-tab category.
- Drag an Item Book item into an inventory or hotbar slot only when in Creative mode or an operator at permission level 2 or higher. This creates the selected stack.
- Inspect crafting-table, furnace, blast-furnace, and campfire recipes.
- Navigate between multiple recipes that produce the selected item.
- Create custom shaped or shapeless crafting recipes, plus furnace, blast-furnace, and campfire cooking recipes.
- Saving a displayed recipe creates a new custom recipe only after its editable content changes.
- Create tag ingredients: hold left Ctrl while hovering an occupied ingredient slot, then select one of the item's tags.
- Remove a default recipe, then restore that exact recipe from the **Removed Recipes** list.
- Remove a custom recipe permanently.
- Hide or reveal an item. Hidden items remain in the Item Book with a **(Hidden)** marker, while their recipes are removed from the server recipe list and compatible JEI views.

The Remove Recipe button is enabled only when a recipe is selected. After removing a default recipe, choose it in **Removed Recipes** before restoring it.

## Resetting recipe changes

```
/recipeeditor restore_all
```

This removes all custom recipes and restores every removed default recipe. It does not reveal hidden items.

## Hiding items

Hiding an item is persistent and destructive. It removes accessible stacks, queues loaded chunks for scanning, removes matching placed block forms, blocks new placement, and rejects matching item entities. Online player storage is rechecked every second; offline-player inventories and unopened or unloaded storage are processed only when accessible. Revealing an item does not restore deleted stacks or blocks. Back up worlds before using it.

## License

This project is licensed under the [MIT License](LICENSE). The complete license notice is included in released mod JARs.
