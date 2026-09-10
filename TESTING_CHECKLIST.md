# In-Game Testing Checklist

Use a disposable test world and a test player with access to Creative mode and operator commands. Back up any world before testing item hiding: hiding is intentionally destructive.

## Test setup

- [x] Start a server with the mod installed.
- [x] Prepare two test players: an operator/Creative player and a non-operator Survival player.
- [x] Collect representative items, including a block item, a non-block item, and several items sharing an item tag.
- [x] Place test items in player inventories, ender chests, a chest or barrel, item frames if relevant, and as dropped item entities.
  - 2026-08-18: Initially failed because an item-frame stack remained; fixed and passed on manual retest.
- [x] Place a test block in both a loaded chunk and a chunk that will be unloaded before the hide test.
- [x] If testing JEI compatibility, install JEI and confirm it loads before beginning.
- [ ] If testing recipe-conflict compatibility, install Polymorph+ and confirm it loads before beginning.

## Automated test coverage

### Current automated suite

- [x] Run `./gradlew test`; all JUnit tests pass.
- [x] Run `./gradlew runGameTestServer`; all registered NeoForge GameTests pass.
- [x] Record the Minecraft, NeoForge, Java, and JEI versions used for the run.
  - 2026-09-10: Minecraft 26.1.1, NeoForge 26.1.1.15-beta, Java 25, JEI API 29.4.0.23. Unit tests and all supported GameTests passed.

### JUnit tests to add

- [ ] Verify saved-data codec round trips defaults, custom recipes, tombstones, and hidden items.
- [ ] Verify legacy `purged_items` saved data is migrated to `hidden_items`.
- [ ] Verify malformed or unreadable persisted snapshots do not prevent valid recipe-editor state from loading.
- [ ] Verify crafting recipe construction trims empty outer rows and columns correctly.
- [ ] Verify shapeless recipe construction retains every non-empty ingredient.
- [ ] Verify recipe construction preserves output count, cooking time, experience, and tag ingredients.
- [ ] Verify invalid layouts, item IDs, recipe types, and counts are rejected without changing saved state.
- [ ] Verify a custom recipe cannot overwrite an existing default or custom recipe ID.

### NeoForge GameTests to add

- [ ] Save a custom shaped recipe, wait for reload completion, and assert it is present in server recipe access.
- [ ] Repeat the live save-and-reload assertion for shapeless, furnace, blast-furnace, and campfire recipes.
- [ ] Remove a default recipe, assert it is absent after reload, restore it, and assert it returns.
- [ ] Reset all recipe changes and assert defaults return, custom recipes are removed, and hidden-item state remains unchanged.
- [ ] Hide an item and assert every recipe producing it is absent from server recipe access.
- [ ] Verify hidden stacks are removed from player inventory, ender chest, carried stack, and open containers.
- [ ] Verify hidden stacks are removed from container block entities and dropped item entities.
- [ ] Verify hidden block placement is canceled and hidden blocks in a loaded chunk are removed.
- [ ] Verify a queued chunk is purged when it becomes available.
- [ ] Verify editor input remains visible while hidden-item purging removes normal player storage.
- [ ] Verify unauthorized or malformed editor actions do not mutate recipes or saved data.

### Continuous integration

- [x] Configure CI to run `./gradlew test` on each pull request.
- [x] Configure CI to run `./gradlew runGameTestServer` on each pull request.
- [x] Publish JUnit and GameTest logs as CI artifacts when a test fails.

## Access and commands

- [ ] As an operator or Creative player, run `/recipeeditor`; the editor opens.
- [ ] As a Survival non-operator, run `/recipeeditor`; access is denied.
- [ ] Change `onlyAdminOrCreative` and `operatorPermissionLevel` in the common config; restart and verify access follows the new settings.
- [ ] From the console or a command block, run `/recipeeditor`; it reports that a player is required.
- [ ] As an authorized player, run `/recipeeditor restore_all`; it reports success and does not remove hidden-item settings.

## Item Book and recipe inspection

- [ ] Open the Item Book and browse multiple creative-tab categories.
- [x] Drag an Item Book item to an inventory or hotbar slot; the cursor preview shows 1 normally and the item's maximum stack size only while Shift is held, matching the placed count.
- [ ] Select an item with no recipe; the editor remains usable and shows no invalid recipe state.
- [ ] Select an item with one supported recipe; its ingredients and output are displayed correctly.
- [ ] Select an item with several recipes; previous/next controls navigate every recipe and wrap correctly.
- [ ] Switch between crafting, furnace, blast-furnace, and campfire modes; only recipes of the selected type appear.
- [ ] Inspect an unsupported recipe type; it is ignored safely rather than breaking the menu.

## Crafting recipes

- [ ] Create a shaped recipe using a full 3x3 pattern; save it and craft it at a crafting table.
- [ ] Create a shaped recipe with empty outer rows or columns; save it and verify its trimmed layout crafts correctly.
- [ ] Create a shapeless recipe; save it and craft it with ingredients in a different order.
- [ ] Save a recipe with an output stack count greater than one; verify the crafted count is correct.
- [ ] Attempt to save an empty crafting layout; it is rejected without creating a recipe.
- [ ] Open a displayed recipe and save without editing it; no duplicate custom recipe is created.

## Polymorph+ compatibility

- [ ] With Polymorph+ installed, create a custom crafting recipe whose ingredient layout conflicts with an existing recipe.
- [ ] Verify Polymorph+ presents each valid result and that selecting each result crafts the intended item.
- [ ] Restart the server and verify the custom recipe and Polymorph+ recipe selection still work.

## Cooking recipes

- [x] Create and use a furnace recipe.
- [x] Create and use a blast-furnace recipe.
- [x] Create and use a campfire recipe.
- [x] For each cooking type, verify input item, output count, cooking time, and experience.
- [x] Attempt invalid inputs or outputs; the server rejects the request without saving a partial recipe.
  - 2026-08-18: Automated GameTest covers invalid air input/output, out-of-range output counts, negative experience, invalid cooking time, and an unsupported recipe type; saved recipe state remains unchanged.

## Tag ingredients

- [ ] Hold left Ctrl while hovering an occupied ingredient slot and select an available item tag.
- [ ] Save a crafting recipe with a tag ingredient; craft it using at least two different items from that tag.
- [ ] Save a cooking recipe with a tag ingredient; process at least two different items from that tag.
- [ ] Reopen the saved recipe; its tag selection is retained rather than converted to the preview item.
- [ ] Restart the server and repeat the prior check.

## Removing and restoring recipes

- [x] Remove a selected default recipe; it is no longer available in-game.
- [ ] If an output has multiple default recipes, remove one and verify only that exact recipe is gone.
- [ ] Open **Removed Recipes**, select the removed entry, and restore it; the exact original recipe returns.
- [ ] Remove a custom recipe; it disappears and cannot be restored as a default recipe.
- [ ] Restart the server after each case; removed defaults remain removed and custom recipes remain present or removed as expected.
- [ ] Run `/recipeeditor restore_all`; default recipes return and all custom recipes are removed.

## Hidden items — destructive tests

- [ ] In a disposable world, hide a non-block item from the editor.
- [ ] Verify all recipes producing it disappear from the editor and the server recipe list.
- [ ] If JEI is installed, verify its compatible recipe views no longer show those recipes.
- [ ] Verify the item remains listed in the Item Book with the **(Hidden)** marker.
- [ ] Verify matching stacks are removed from an online player's inventory, ender chest, cursor, and currently open container.
- [x] Verify matching dropped item entities are removed and newly spawned item entities do not enter the world.
  - 2026-08-18: Initially failed for multiple matching items dropped at the same position; fixed and passed on manual retest.
- [ ] Hide a block item; verify matching placed blocks in loaded chunks are removed.
- [ ] Try to place the hidden block; placement is prevented.
- [ ] Load a chunk that was unloaded while the item was hidden; verify matching container stacks and placed block forms are purged.
- [ ] Join with a player whose inventory contained the item while offline; verify accessible player storage is purged on login.
- [ ] Reveal the item; it becomes usable again, but previously deleted stacks and blocks do not return.

## Multiplayer and resilience

- [ ] Open the editor with two authorized players for the same output item.
- [ ] Have one player save, remove, or restore a recipe while the other is viewing it; both menus remain usable after refresh.
- [ ] Close the editor during a save or removal; the server completes or rejects the request cleanly with no corrupted state.
- [ ] Change a player's permission while the editor is open; later edits are rejected if access is no longer allowed.
- [ ] Restart after a mixture of custom recipes, removed defaults, and hidden items; each persisted state is reapplied correctly.
- [ ] Check server logs after the suite; there are no unexpected exceptions or recipe-reload failures.

## Release sign-off

- [ ] All applicable checks above pass on a clean test world.
- [ ] Hidden-item tests were run only on a disposable world or backed-up world.
- [ ] Test results, Minecraft version, NeoForge version, JEI version (if used), and any failures are recorded.
