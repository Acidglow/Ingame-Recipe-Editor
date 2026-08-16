# Acidglow's Ingame Recipe Editor

A server-authoritative NeoForge mod for Minecraft 26.2 that lets permitted players inspect, create, remove, restore, and hide recipes without leaving the game. Recipe changes are stored with the world and reapplied whenever its recipes reload.

## Requirements

- Minecraft 26.2
- NeoForge 26.2.0.59 or newer

## Opening the editor

Use the following command in-game:

```
/recipeeditor
```

By default, only Creative-mode players and operators at permission level 2 or higher can use it. The common configuration can change both the restriction and the required operator level.

## What the editor supports

- Browse registered items in the Item Book, grouped by creative-tab category.
- Inspect crafting-table, furnace, blast-furnace, and campfire recipes.
- Navigate between multiple recipes that produce the selected item.
- Create custom shaped or shapeless crafting recipes, plus furnace, blast-furnace, and campfire cooking recipes.
- Saving a displayed recipe creates a new custom recipe only after you change its editable content; an unchanged recipe is not duplicated.
- Create tag ingredients: hold left Ctrl while hovering an occupied ingredient slot, then select one of the item's tags. Saving preserves existing tag ingredients and uses newly selected tags rather than their individual preview items.
- Remove a default recipe, then restore that exact recipe from the **Removed Recipes** list.
- Remove a custom recipe permanently. It cannot be restored as a default recipe because it did not come from the game's original recipe data.
- Hide or reveal an item. Hidden items remain in the Item Book with a **(Hidden)** marker, while their recipes are removed from the server recipe list and compatible JEI views.

The Remove Recipe button is enabled only when a recipe is currently selected. After removing a default recipe, use the Removed Recipes button to choose the specific recipe to restore; the button will then become Restore Default Recipe.

## Resetting recipe changes

```
/recipeeditor restore_all
```

This removes all custom recipes and restores every removed default recipe. It does not reveal hidden items; reveal those individually in the editor.

## Important note about hiding items

Hiding an item is persistent and destructive: it immediately purges accessible stacks, queued loaded chunks, container block entities, and matching placed block forms. It prevents players from placing the hidden block and stops hidden item entities from joining the world. Each unloaded chunk is scanned when it loads, and online player storage is rechecked every second. Offline-player inventories and unopened/unloaded storage are purged when they become accessible, not by force-loading the whole world. Revealing an item does not restore anything already deleted.
