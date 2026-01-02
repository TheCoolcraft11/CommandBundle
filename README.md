# CommandBundle

A Paper/Spigot plugin that lets you define **custom commands** ("bundles") – named commands whose actions are executed
in sequence, with delays, conditions, variables, webhooks, and more.

---

## Overview

A **bundle** is a named custom command defined in `plugins/CommandBundle/commands.yml`.

- Each bundle becomes its own command, e.g. a bundle named `starter` is available as `/starter`.
- Bundles can have **subcommands**, e.g. `/starter confirm`.
- Each bundle/subcommand is made of **action lines** that can:
    - Run commands as the player or console.
    - Send messages.
    - Use delays and conditions.
    - Work with variables and math.
    - Call host OS commands (optional) and HTTP webhooks (optional).

Typical use cases:

- Granting ranks or kits with several commands (permissions, items, effects).
- Starting events (teleporting players, broadcasting messages, toggling settings).
- Quick admin actions like mute + warn + broadcast.
- Integrating with external systems via webhooks or host commands.

For a full reference of the **action syntax** you can use inside `commands.yml`, see `FEATURES.md`.

---

## Features

- Define and manage named bundles via `/bundle`.
- Each bundle is registered as a **real command** on the server.
- Player-friendly listing and info views for bundles.
- Support for multiple **subcommands** per bundle.
- Per-bundle permission configuration so only specific groups can use a bundle.
- Granular admin permission system for managing bundles:
    - `commandbundle.*` wildcard for full access.
    - Separate permissions for adding, removing, listing, viewing, reloading, editing, and managing
      subcommands/permissions.
- Command actions support:
    - Delays, conditions, random selection.
    - Variables and simple JSON field access.
    - Loops over lists of values.
    - Host OS commands (if enabled).
    - HTTP webhooks (if enabled).

Details of the action syntax live in `FEATURES.md`.

---

## Installation

1. Download the plugin JAR.
2. Place the JAR into your server's `plugins/` directory.
3. Start or restart your server.
4. Confirm the plugin is loaded:
    - Run `/plugins` and look for **CommandBundle**.
    - Check your console for any startup errors.
5. On first run the plugin creates its data folder:
    - `plugins/CommandBundle/config.yml` – global settings.
    - `plugins/CommandBundle/commands.yml` – your bundles/custom commands (initially empty).

---

## Commands & Usage

The management command is `/bundle`.

- **Base command**: `/bundle`
- **Aliases**: `/cmdbundle`, `/cb`
- **Usage**: `/bundle <add|remove|list|info|edit|subcommand|permission|reload|help>`

### `/bundle add <name> <action...>`

Create a new bundle with the given name.

- **Purpose**: Create a new bundle and define its base actions.
- **Permission**: `commandbundle.add`
- **Syntax**:
    - `/bundle add <name> <action1> [| <action2> | <action3> ...]`
    - Actions are separated by `|` and each action is a full **action line** (the same as in `commands.yml`).
- **Example**:
    - `/bundle add starter !give %player% stone_sword 1 | #message:green:Enjoy your starter kit!`

This creates a `starter` bundle and immediately registers `/starter` as a command.

### `/bundle remove <name>`

Delete an existing bundle and unregister its command.

- **Purpose**: Permanently remove a bundle.
- **Permission**: `commandbundle.remove`
- **Example**:
    - `/bundle remove starter`

### `/bundle list`

List available bundles.

- **Purpose**: View existing bundle names and how many actions they contain.
- **Permission**: `commandbundle.list`
- **Example**:
    - `/bundle list`

### `/bundle info <name>`

Show the raw action lines for a bundle.

- **Purpose**: Inspect how a bundle is defined.
- **Permission**: `commandbundle.info`
- **Example**:
    - `/bundle info starter`

### `/bundle edit <name> <add|insert|remove|replace> ...`

Edit the action list of an existing bundle.

- **Permission**: `commandbundle.edit`
- **Operations**:
    - `add <action>` – append a new action at the end.
    - `insert <index> <action>` – insert at zero-based index.
    - `remove <index>` – remove the action at index.
    - `replace <index> <action>` – replace the action at index.

Example:

- `/bundle edit starter add #message:yellow:You used the starter bundle again!`
- `/bundle edit starter replace 0 !give %player% stone_pickaxe 1`

### `/bundle subcommand <name> <sub> <action...>`

Define or overwrite a **subcommand** for a bundle.

- **Purpose**: Add separate action lists for `/name <sub>`.
- **Permission**: `commandbundle.subcommand`
- **Syntax**:
    - `/bundle subcommand <name> <sub> <action1> [| <action2> ...]`
- **Example**:
    - `/bundle subcommand starter confirm #message:green:Starter confirmed!`

After this, `/starter confirm` will run the subcommand actions instead of the base `starter` actions.

### `/bundle permission <name> [permission.node]`

Get or set the **permission node required to use** a bundle.

- **Purpose**: Restrict or open access to a bundle.
- **Permission**: `commandbundle.permission`
- **Behavior**:
    - Without `permission.node`, shows the current permission (if any).
    - With `permission.node`, sets or clears the permission stored in `commands.yml`.
- **Example**:
    - `/bundle permission starter server.bundle.starter`

When a permission is set, players must have that node to use `/starter`.

### `/bundle reload`

Reload bundle definitions from `commands.yml`.

- **Purpose**: Apply changes to bundles made in `commands.yml` without restarting the server.
- **Permission**: `commandbundle.reload` (typically OP only).
- **Example**:
    - `/bundle reload`

> Note: `/bundle reload` reloads **commands.yml** (bundles), not `config.yml`. To apply changes in `config.yml`, restart
> the server or reload the plugin with your server's plugin manager.

### `/bundle help [topic]`

Show in-game help for bundle features.

- **Permission**: usually same as `commandbundle.add`/`commandbundle.edit`.
- **Topics** (if provided):
    - `placeholders` – argument and player/server placeholders.
    - `conditions` – `[if:]`, `[else if:]`, `[else]`.
    - `delays` – `[delay:]`.
    - `variables` – `+name:value` and `%var:name%`.
    - `random` – `[random]` / `[random:<weight>]`.
    - `subcommands` – subcommand basics.
    - `edit` – `edit` usage.

---

## Using Bundles

Once a bundle exists in `commands.yml` or via `/bundle add`, it becomes a normal command:

- Bundle name `starter` → command `/starter`.
- With subcommand `confirm` → `/starter confirm`.

Bundles use the **syntax** documented in `FEATURES.md`. In short, actions can:

- Run commands as the player or console, optionally silently.
- Send rich colored messages.
- Use `[delay:seconds]` to schedule actions later.
- Use `[if:type:value]`, `[else if:...]`, `[else]` for branching.
- Use `[random]` / `[random:weight]` to pick one of several options.
- Set and read variables (`+name:value`, `%var:name%`, JSON field access).
- Loop over lists (`[foreach:list:var]`).
- Read/write files (`,,file` / `;;file`).
- Execute host system commands (`$...`) and HTTP webhooks (`%...`) if enabled in `config.yml`.

See `FEATURES.md` for full syntax and examples.

---

## Permissions

CommandBundle uses a granular permission system. You can grant individual permissions or use the wildcard for full
access.

- `commandbundle.*`
    - Grants all CommandBundle permissions.
- `commandbundle.add`
    - Allows adding new bundles.
- `commandbundle.remove`
    - Allows removing bundles.
- `commandbundle.list`
    - Allows listing bundles.
- `commandbundle.info`
    - Allows viewing bundle details.
- `commandbundle.reload`
    - Allows reloading bundle definitions from `commands.yml`.
- `commandbundle.edit`
    - Allows editing existing bundles.
- `commandbundle.subcommand`
    - Allows adding or modifying subcommands for bundles.
- `commandbundle.permission`
    - Allows setting per-bundle use permissions.

> Exact defaults (`true` vs `op`) are defined in `plugin.yml` and may differ between versions.

In addition, each bundle can define its own **use permission** in `commands.yml`:

```yml
commands:
  starter:
    permission: server.bundle.starter
    actions:
      - "#message:green:You used /starter!"
```

If `permission` is set, CommandBundle will check it before executing the bundle's actions.

---

## Configuration

The plugin's main configuration file is:

- `plugins/CommandBundle/config.yml`

As of the current implementation, these options are especially important:

- `blacklisted-commands` (list of strings)
    - Base command names that **cannot** be executed by bundles (e.g. `stop`, `reload`).
- `host-commands-enabled` (boolean)
    - Enables or disables host OS commands started with `$` and command substitution with `&(...)`.
- `webhooks-enabled` (boolean)
    - Enables or disables webhook actions starting with `%`.

Other options may exist (e.g. debug or logging toggles) depending on plugin version; check the generated
`config.yml` and its comments for details.

Bundle definitions themselves are stored in:

- `plugins/CommandBundle/commands.yml`

Structure (simplified):

```yml
commands:
  starter:
    permission: server.bundle.starter
    actions:
      - "#message:green:You used /starter!"
    subcommands:
      confirm:
        - "#message:yellow:You confirmed /starter!"
```

Every entry in an `actions` or `subcommands.<name>` list is a single **action line** parsed and executed by
CommandBundle.

For exact action syntax, see `FEATURES.md`.

---

## Further Documentation

- `FEATURES.md` – full reference for bundle action syntax (delays, conditions, variables, loops, webhooks, host
  commands, placeholders, math, file I/O, and more).
- Source code (for developers) – see the classes:
    - `CustomCommandManager`
    - `CommandAction`
    - `ConditionEvaluator`
    - `MathEvaluator`
    - `VariableManager`
    - `WebhookData`

