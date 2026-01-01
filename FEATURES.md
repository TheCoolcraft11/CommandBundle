# Command Bundles – Action Syntax & Capabilities

This document explains **what a bundle action line can do** – the syntax you can use inside
`plugins/CommandBundle/commands.yml` and how each feature behaves when the bundle runs.

It does **not** cover plugin installation, permissions, or the `/bundle` management command. See `README.md` for that.

---

## 1. Bundle Structure (High Level)

Bundles are stored in `commands.yml` like this:

```yml
commands:
  example:
    permission: my.plugin.example
    actions:
      - "...action line 1..."
      - "...action line 2..."
    subcommands:
      sub1:
        - "...action for /example sub1..."
      sub2:
        - "...action for /example sub2..."
```

- Each entry in `actions:` (and in each `subcommands:` list) is a **single action line**.
- An action line can contain:
    - Prefixes like `[delay:]`, `[if:]`, `[else if:]`, `[else]`, `[random]`, `[foreach:]`.
    - Special markers like `!`, `-`, `+`, `$`, `%`, `#message`.
    - Placeholders like `%player%`, `%var:name%`, `%players%`, `{math:...}`.

The rest of this file documents these pieces.

---

## 2. Timing & Ordering

### 2.1 Delayed Actions: `[delay:<seconds>]`

Add a delay **before** running this action.

```text
[delay:5]say This line runs 5 seconds after the previous one
```

- Time is in **seconds**.
- Delays accumulate: if one action uses `[delay:5]` and the next uses `[delay:3]`, the second will run 8 seconds after
  the first non-delayed action.

If no `[delay:]` is present, the action runs immediately (relative to the timeline built by earlier delays).

---

## 3. Conditions & Branching

Conditions use `ConditionEvaluator` under the hood and are expressed as text inside `[if:...]` or `[else if:...]`.

### 3.1 Condition Syntax

Action lines support `if / else if / else` chains across multiple lines:

```text
[if:<type>:<value>[:<extra>]]<action>
[else if:<type>:<value>[:<extra>]]<action>
[else]<action>
```

Where `<type>` is one of the supported condition types below.

- If an `[if:...]` condition is **false**, that action is skipped.
- `[else if:...]` is evaluated only if all previous `if/else if` in the chain were false.
- `[else]` runs only if all previous `if/else if` in the chain were false.

The plugin does not parse full expressions like `%playercount% > 5`. Instead, it uses **typed** conditions.

### 3.2 Supported Condition Types

From `ConditionEvaluator`:

- `permission` / `perm`
    - Checks if sender has a permission.
    - `permission:<node>`
    - Example: `[if:permission:myplugin.vip]#message:green:You are VIP!`

- `item`
    - Checks if a player has an item (optionally with amount).
    - `item:<material>[:<amount>]`
    - Example: `[if:item:DIAMOND:10]#message:yellow:You have at least 10 diamonds!`

- `world`
    - Checks the player's world name.
    - `world:<worldName>`

- `gamemode` / `gm`
    - Checks the player's game mode.
    - `gamemode:<SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR>`

- `health`
    - Numeric comparison on player's health.
    - `health:<comparison>` where `<comparison>` is one of:
        - `>N`, `>=N`, `<N`, `<=N`, `=N`, `==N`, or just `N` (interpreted as `>=N`).
    - Example: `[if:health:>10]#message:green:You are healthy!`

- `level` / `xp`
    - Numeric comparison on player's experience level.
    - Same comparison rules as `health`.

- `flying`, `sneaking`
    - Boolean comparison.
    - `flying:true` or `flying:false`.
    - Example: `[if:flying:true]#message:yellow:You are flying!`

- `op`
    - Checks whether `sender.isOp()` matches the boolean value.
    - `op:true` or `op:false`.

- `player`
    - Compares the player's name.
    - `player:<name>`

- `var`
    - Compares a stored variable to an expected value.
    - `var:<name>:<expected>`
    - Example: `[if:var:rank:vip]#message:gold:VIP perks active!`

> **Note:** For player-dependent types (`item`, `world`, `gamemode`, `health`, `level`, `flying`, `sneaking`,
> `player`), conditions will be **false** when run from the console.

---

## 4. Randomized Actions: `[random]` / `[random:<weight>]`

You can mark multiple actions as **random options**. From all random actions in the current execution list, only one is
chosen.

```text
[random]#message:yellow:You got message A
[random]#message:yellow:You got message B
[random:10]#message:gold:You got rare message C
```

- All actions with `[random]` or `[random:<weight>]` are collected.
- One is chosen based on `randomWeight` (default `100`, or your specified number).
- Non-random actions in the same list run normally alongside the selected random one.

---

## 5. Console vs Player Execution & Output Suppression

### 5.1 Console Command: `!`

Prefix the actual command with `!` to run it as **console** instead of the sender.

```text
!say This is executed by the console
!lp user %player% parent set vip
```

Without `!`, the command is dispatched as the **sender** of the bundle.

### 5.2 Suppressing Command Output: `-`

Prefix the action with `-` to suppress output for that command.

```text
-!say This will run as console, but output is suppressed
-eco give %player% 100
```

- Internally, `executeCommandSilently` temporarily redirects `System.out` / `System.err` while running the command.
- You can combine `-` with `!` or other modifiers; `-` is handled before command dispatch.
- Suppression only affects visible output – the command still runs.

---

## 6. Messages Without Commands: `#message`

Use `#message` to send chat messages directly instead of running a command.

```text
#message:green:This goes to the command sender in green
#message@Steve:yellow:Hello Steve!
```

Syntax:

```text
#message[@<targetPlayer>]:<colorSpec>:<text>
```

- `@<targetPlayer>` (optional): send to a specific player name/placeholder instead of the sender.
- `<colorSpec>`: comma-separated color/style tokens (e.g. `green`, `gold,BOLD`).
- `<text>`: actual message text; supports all placeholders.

`<colorSpec>` is interpreted via Adventure's `NamedTextColor` and decorations:

- Colors: `BLACK`, `DARK_BLUE`, `DARK_GREEN`, `DARK_RED`, `DARK_PURPLE`, `GOLD`, `GRAY`, `DARK_GRAY`, `BLUE`,
  `GREEN`, `RED`, `LIGHT_PURPLE`, `YELLOW`, `WHITE`.
- Styles: `BOLD`, `ITALIC`, `UNDERLINED`, `STRIKETHROUGH`, `OBFUSCATED`.

Example:

```text
#message:red:You do not have access to this bundle!
#message@%player%:gold,BOLD:You triggered a special bundle!
```

---

## 7. Variables & Storage: `+` and `%var:...%`

You can store values into variables and reuse them later.

### 7.1 Set Variable: `+name:value`

```text
+myVar:Hello
#message:green:Value is %var:myVar%
```

- For players, variables are **per-player** (keyed by UUID).
- For console senders, variables are **global**.
- `name` and `value` both support placeholders and will be processed before storage.

### 7.2 Suppress Variable Feedback: `~`

Append `~` to the variable name to avoid the "Variable set" chat message:

```text
+secretVar~:hidden value
```

In this example, `secretVar` is stored but the sender is not notified.

### 7.3 Reading Variables: `%var:...%`

Variables are read through placeholder syntax handled by `VariableManager` and the helper methods
`replaceVariables` / `replaceVariablesForConsole`:

```text
#message:yellow:Your stored value is %var:myVar%
```

#### 7.3.1 JSON Path Access

If a variable contains a JSON-like string, you can access nested fields using dot-notation:

```text
#message:gold:Your profile name is %var:profile.name%
```

- `profile` is the variable name.
- `name` is the JSON key.
- Multiple levels are supported: `profile.stats.kills`.

The JSON parsing is simple and string-based – it works best with well-formed JSON and simple structures.

#### 7.3.2 Fallback Variable Resolution

Placeholders like `%SomeName%` that are **not** reserved (e.g. not `player`, `args`, `players`, `var`, etc.) are also
resolved as variables:

- First from the player's variables.
- Then from global variables.

This allows you to write `%MyCustomVar%` without the `var:` prefix in many cases.

---

## 8. Host System Commands: `$`

Actions starting with `$` execute **host OS commands** (shell) instead of Minecraft commands.

```text
$echo Hello from shell
$ls -1 plugins
```

> Host commands only run if `host-commands-enabled: true` in `config.yml`.

### 8.1 Storing Host Command Output: `>>variable`

Append `>>variableName` to capture the shell output into a variable:

```text
$ls -1 plugins >>plugin_list
#message:gray:Plugins on host: %var:plugin_list%
```

- For player senders, the variable is stored as a **player variable**.
- For console, it becomes a **global variable**.
- If `suppressCommandOutput` is **false**, the plugin prints either the captured output or a success message.
- Combine with `-` to suppress all extra messages.

### 8.2 Command Substitution: `&(...)`

Command substitution runs a host command and inlines its output into the text.

```text
#message:gray:Host date is &(date +"%Y-%m-%d")
```

- Pattern: `&(<shell command>)`.
- Only available if `host-commands-enabled: true`.
- Output lines are joined with spaces and substituted into the surrounding text.
- If substitution fails, an empty string is inserted and a warning is logged.

> The earlier `@(command)` syntax you might see in older docs is **not** used by the current implementation; use
> `&(...)` instead.

---

## 9. Webhooks / HTTP Calls: `%`

Actions starting with `%` represent **webhook calls**. The string after `%` is parsed by `WebhookData.parse(...)`.

General format:

```text
%<url>[>><varName>][::Header1:Value1,Header2:Value2][::body]
```

Examples:

```text
%https://example.com/hook
%https://example.com/hook>>responseVar
%https://example.com/hook::Content-Type:application/json::{"player":"%player%"}
%https://example.com/hook>>!silentVar::Content-Type:application/json::{"uuid":"%uuid%"}
```

- `url` – required, parsed into a `java.net.URI` and called via HTTP `POST`.
- `>>varName` – optional; store the response body into a variable:
    - With a leading `!` (e.g. `>>!varName`), the webhook is **silent** (no chat message about storage).
    - If `varName` itself contains `%...%`, it is treated as a **dynamic variable name**, resolved at runtime.
- `::Header1:Value1,Header2:Value2` – optional headers.
- `::body` – optional request body (string), processed through placeholders.

Behavior (from `executeWebhook`):

- Always uses HTTP `POST`.
- 2xx responses are treated as success; body is read into a string.
- If `>>varName` is set and the sender is a player, the response is stored as a **player variable** under that name.
- If no variable is set but webhook is not silent, the response (or error) is sent to the sender as a chat message.

> Webhooks only run if `webhooks-enabled: true` in `config.yml`.

---

## 10. Loops / Foreach: `[foreach:list:var]`

You can loop over a list of values and run an action once per item.

```text
[foreach:Player1,Player2,Player3:player]say Hello %player%
```

Syntax:

```text
[foreach:<list>:<variable>] <action using %<variable>%>
```

- `<list>`: comma-separated values, after placeholder replacement.
- `<variable>`: the placeholder name you’ll use inside the action.

Execution (from `executeLoopAction`):

1. `<list>` is processed through `replacePlaceholders`.
2. If it **starts with** `&(`, it is intended to come from command substitution and is split by commas/newlines.
3. Otherwise, it is split by commas.
4. For each item:
    - Whitespace is trimmed; empty items are skipped.
    - `%<variable>%` is replaced with the item inside the action template.
    - The result is processed again with full placeholder replacement.
    - The final command is executed (as the sender).

Examples:

```text
[foreach:%players%:player]#message:green:Hello %player%
[foreach:%teams%:team]#message:yellow:Team found: %team%
```

---

## 11. Placeholders & Dynamic Values

All non-control parts of an action line go through `replacePlaceholders(...)` before execution. This includes:

1. Processing escape sequences.
2. Argument placeholders.
3. Player-specific placeholders.
4. Variable placeholders.
5. Server/team placeholders.
6. File read/write helpers.
7. Command substitution.
8. Math expressions.
9. Un-escaping preserved characters.

### 11.1 Argument Placeholders

If your bundle command is `/mybundle arg1 arg2`, you can access arguments as:

- `%args%` – all arguments joined by spaces.
- `%arg1%`, `%arg2%`, ... – individual arguments.

There are also more advanced patterns implemented in `replaceArguments`:

- `%argN-%` – remaining arguments from position `N` to the end (or empty if not present).
- `%argN-::<default>%` – remaining arguments from position `N`, or `<default>` if not present.
- `%argN::<default>%` – single argument at `N`, or `<default>` if not present.

Examples:

```text
#message:yellow:First argument: %arg1%.
#message:yellow:Rest: %arg2-%.
#message:yellow:Optional arg: %arg3::none%
```

### 11.2 Player Context

When executed by a player, these placeholders are available:

- `%player%` – player name
- `%uuid%`, `%player_uuid%` – UUID
- `%world%` – world name
- `%x%`, `%y%`, `%z%` – block coordinates
- `%health%` – current health
- `%level%` – XP level
- `%gamemode%` – game mode name

Plus any **player variables** set earlier (via `+` or webhooks/host commands).

### 11.3 Server-Wide Placeholders

Available regardless of player/console:

- `%players%` – comma-separated list of online player names.
- `%players_uuid%` – comma-separated list of online player UUIDs.
- `%playercount%` – number of online players.
- `%teams%` – comma-separated list of scoreboard team names.
- `%teamplayers:<team>%` – comma-separated players in a team.
- `%teamplayers_uuid:<team>%` – comma-separated UUIDs in a team.

Team placeholders also support nesting – the team name itself can contain placeholders. The code resolves any nested
`%...%` before looking up team members.

### 11.4 File Read Helpers: `,,`

`replaceFileRead` provides a compact way to read file contents or YAML values into the text.

Pattern:

```text
,,<path>[::<yamlPath>]
```

Examples:

```text
#message:gray:Config value: ,,config.yml::mysetting.path
#message:gray:File contents: ,,notes.txt
```

- If `<path>` is not absolute, it is resolved relative to the plugin data folder.
- If `::<yamlPath>` is present, the file is loaded as YAML and the value at `yamlPath` is inserted.
- Otherwise, the entire file is read as text and inserted.

### 11.5 File Write Helpers: `;;`

`handleFileWrite` lets you write to files or YAML directly from actions.

Pattern:

```text
;;<path>::<content>
;;<path>::<yamlPath>::<value>
```

Examples:

```text
;;notes.txt::Hello world
;;config.yml::mysetting.enabled::true
```

- If `<path>` is not absolute, it is resolved relative to the plugin data folder.
- For YAML writes (`::<yamlPath>::<value>`):
    - Existing file is loaded if present; otherwise a new YAML file is created.
    - `value` is parsed as int/double/boolean when possible, or stored as a string.
- For plain writes (`::<content>`):
    - File contents are replaced by `content`.

---

## 12. Math Expressions: `{math:...}`

Math expressions are handled by `MathEvaluator` and applied via `replaceMathExpressions(...)`.

Pattern:

```text
{math:<expression>}
```

Examples:

```text
#message:yellow:Two plus two is {math:2+2}
#message:yellow:Level next: {math:%level%+1}
```

`MathEvaluator` supports:

- Basic operators: `+`, `-`, `*`, `/`, `%`, `^`.
- Ranges: `A..B` – expanded as a numeric range (e.g. `1..3` → `1,2,3`).
- Functions: `sqrt(x)`, `int(x)` (truncate), `round(x)`.

The expression receives placeholders **after** other replacements (so `%level%` in the example above is already a
number).

---

## 13. Putting It All Together – Example Action Lines

Below are some examples that combine multiple features.

### 13.1 Delayed, Console, Variable, and Message

```text
[delay:3]+welcomeMsg:Welcome %player%!
[delay:3]#message:green:%var:welcomeMsg%
[delay:4]-!say [LOG] Welcome message sent to %player%
```

### 13.2 Webhook + Variable + Condition

```text
%https://api.example.com/profile>>profileJson::Content-Type:application/json::{"uuid":"%uuid%"}
[if:var:profileJson.name:!=]
#message:gold:Welcome back, %var:profileJson.name%!
[else]
#message:yellow:Welcome, new player!
```

*(Adjust the condition to your actual data/structure – conditions are type/value based, not full expressions.)*

### 13.3 Host Command + Loop

```text
$ls -1 players >>playerFiles
[foreach:%var:playerFiles%:file]#message:gray:Found data file: %file%
```

These examples should give you a **feature-complete view of what bundle action lines can do**. For the exact
implementation details, see the source classes:

- `CustomCommandManager.java`
- `CommandAction.java`
- `ConditionEvaluator.java`
- `MathEvaluator.java`
- `VariableManager.java`
- `WebhookData.java`
