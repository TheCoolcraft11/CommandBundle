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
    - **Advanced AND/OR logic** for combining multiple conditions.
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
5. On first run the plugin creates its data folder structure:
    - `plugins/CommandBundle/config.yml` – global settings.
    - `plugins/CommandBundle/commands/` – directory for command files.
    - `plugins/CommandBundle/commands/commands.yml` – default file for bundles (created on first `/bundle add`).

You can create additional YAML files in the `commands/` directory to organize your commands. All `.yml` files
will be automatically loaded and merged at startup.

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

### File Management Commands

These commands manage which command files are loaded and when.

#### `/bundle loadfile <filename.yml>`

Temporarily load commands from a specific file.

- **Purpose**: Load a command file for the current session without permanently enabling it.
- **Permission**: `commandbundle.reload`
- **Example**:
    - `/bundle loadfile events.yml`
- **Behavior**:
    - Commands are loaded into memory immediately
    - NOT saved to config.yml
    - File will NOT be loaded on next server restart

#### `/bundle enablefile <filename.yml>`

Enable a command file for auto-loading on server startup.

- **Purpose**: Permanently enable a file to be loaded every time the server starts.
- **Permission**: `commandbundle.reload`
- **Example**:
    - `/bundle enablefile events.yml`
- **Behavior**:
    - Saves the setting to config.yml
    - Immediately loads the file
    - File will be auto-loaded on every restart

#### `/bundle disablefile <filename.yml>`

Disable a command file from being auto-loaded.

- **Purpose**: Stop a file from being auto-loaded at startup.
- **Permission**: `commandbundle.reload`
- **Example**:
    - `/bundle disablefile events.yml`
- **Behavior**:
    - Saves the setting to config.yml
    - Does NOT unload currently loaded commands
    - File will NOT load on next restart

#### `/bundle unloadfile <filename.yml>`

Unload commands from a specific file.

- **Purpose**: Remove commands from memory that were loaded from a specific file.
- **Permission**: `commandbundle.reload`
- **Example**:
    - `/bundle unloadfile events.yml`
- **Behavior**:
    - Removes commands from memory immediately
    - Does NOT change config.yml
    - Useful for testing or temporary changes

### `/bundle help [topic]`

Show in-game help for bundle features.

- **Permission**: usually same as `commandbundle.add`/`commandbundle.edit`.
- **Topics** (if provided):

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
- Use `[branch]...[endbranch]` with `[if:type:value]`, `[else if:...]`, `[else]` for multi-line branching.
- Use `[random]` / `[random:weight]` to pick one of several options.
- Use `[CONDSTART][AND]` or `[CONDSTART][OR]` to combine multiple conditions with AND/OR logic.

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
- `default-commands-file` (string, default: `commands.yml`)
    - Specifies the default file name used when saving commands via the `/bundle` in-game command.
    - Commands are saved to `plugins/CommandBundle/commands/<filename>`.

Other options may exist (e.g. debug or logging toggles) depending on plugin version; check the generated
`config.yml` and its comments for details.

### Command Files & Directory Structure

Bundle definitions are stored in the **commands directory**:

- `plugins/CommandBundle/commands/`
    - `commands.yml` (default, created when using `/bundle add` or `/bundle edit`)
    - `custom_commands.yml` (or any other `*.yml` file you create)

**How it works**:

- The plugin automatically scans `plugins/CommandBundle/commands/` for all `.yml` files.
- All command definitions from all files are **merged into memory** at startup.
- When you use `/bundle add` or `/bundle edit` commands, they are saved to the **default commands file** (configured by
  `default-commands-file`).
- If a command name appears in multiple files, the first occurrence is used and duplicates are skipped (with a warning).
- By default only `default-commands-file` is loaded on startup, but you can use `/bundle loadfile` and
  `/bundle enablefile` to load additional files, or enable `auto-load-commands` in `config.yml` to load all files
  automatically.

**Example YAML structure** (simplified):

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

