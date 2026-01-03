## 1. Bundle Structure (High Level)

Bundles are stored in YAML files within the `plugins/CommandBundle/commands/` directory. You can use multiple `.yml`
files to organize your commands. All files are automatically loaded and merged at startup (unless `auto-load-commands`
is disabled in `config.yml`).

**Example structure:**

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

**Files organization:**

- Store bundles in any `.yml` file in the `commands/` directory (e.g., `commands.yml`, `events.yml`, `admin.yml`)
- When using `/bundle add` command, bundles are saved to the `default-commands-file` (default: `commands.yml`)
- All `.yml` files are merged into a single set of commands at runtime
- Use `/bundle loadfile`, `/bundle enablefile`, and `/bundle disablefile` to manage which files are loaded

**Action lines structure:**

- Each entry in `actions:` (and in each `subcommands:` list) is a **single action line**.
- An action line can contain:
    - **Timing prefixes**: `[delay:]`
    - **Branching prefixes**: `[if:]`, `[else if:]`, `[else]`, `[branch]`, `[endbranch]`
    - **Logic prefixes**: `[AND]`, `[OR]`, `[CONDSTART]`, `[CONDEND]`
    - **Special markers**: `!` (console), `-` (suppress output), `+` (variables), `$` (host commands), `%` (webhooks),
      `#message` (messages)
    - **File helpers**: `,,` (read files), `;;` (write files)
    - **Modifiers**: `[random]`, `[foreach:]`
    - **Placeholders**: `%player%`, `%var:name%`, `%players%`, `{math:...}`

The rest of this file documents these pieces in detail.

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

### 3.1 Branch Blocks

To create multi-line if/else chains, wrap them in `[branch]` and `[endbranch]` markers:

```text
[branch]
[if:<type>:<value>[:<extra>]]
<action when if is true>
[else if:<type>:<value>[:<extra>]]
<action when else if is true>
[else]
<action when nothing matched>
[endbranch]
```

**How it works:**

- `[branch]` starts a conditional block
- `[if:...]` tests the first condition; if true, actions following it run until the next condition or `[endbranch]`
- `[else if:...]` tests only if no prior condition matched; can have multiple `[else if]` clauses
- `[else]` runs only if no condition matched
- `[endbranch]` ends the block
- Only one branch executes; once a condition matches, all others are skipped

**Example - Multiple conditions:**

```text
[branch]
[if:var:playerJSON.name:!=]
[if:var:playerJSON.level:!=]
#message:gold:Welcome back %var:playerJSON.name%, level %var:playerJSON.level%!
[else if:var:playerJSON.name:!=]
#message:yellow:Welcome back %var:playerJSON.name%! (No level data)
[else]
#message:red:New player detected!
[endbranch]
```

The plugin does not parse full expressions like `%playercount% > 5`. Instead, it uses **typed** conditions.

### 3.1.1 Advanced: AND/OR Logic for Multiple Conditions

You can combine multiple conditions using **AND** or **OR** logic. By default, multiple `[if:...]` conditions in a chain
use **OR** logic (any condition can be true). To require **all** conditions to be true, use **AND** logic.

**Using `[CONDSTART]` and `[CONDEND]`:**

All conditions in a chain must be on a **single action line** with the `[CONDSTART]` and `[CONDEND]` markers:

```text
[CONDSTART][OR][if:<condition1>][if:<condition2>][if:<condition3>][CONDEND]
<action when any condition is true>
```

```text
[CONDSTART][AND][if:<condition1>][if:<condition2>][if:<condition3>][CONDEND]
<action when all conditions are true>
```

**How it works:**

- `[CONDSTART]` marks the beginning of a condition chain
- `[AND]` or `[OR]` specifies the logic mode (default is `[OR]` if not specified)
- All `[if:...]` conditions must be on the same line as `[CONDSTART]` and `[CONDEND]`
- `[CONDEND]` closes the chain and the following action executes if the combined condition is met
- This works both inside and outside of `[branch]` blocks

**Example - OR Logic (any condition matches):**

```text
[CONDSTART][OR][if:permission:vip.tier1][if:permission:vip.tier2][if:permission:vip.tier3][CONDEND]
#message:gold:VIP access granted!
!give %player% diamond 5
```

In this example, the player needs **any one** of the three VIP permissions to receive the rewards.

**Example - AND Logic (all conditions must match):**

```text
[CONDSTART][AND][if:level:>=20][if:health:>15][if:world:adventure_world][CONDEND]
#message:green:All requirements met! Quest unlocked!
!give %player% enchanted_book 1
```

In this example, the player must satisfy **all three** conditions: be level 20+, have health >15, AND be in the
adventure_world.

**Example - Inside a Branch Block:**

```text
[branch]
[CONDSTART][AND][if:permission:quest.access][if:level:>=10][if:var:quest_completed:false][CONDEND]
#message:gold:Starting quest!
+quest_completed:true
[else if:var:quest_completed:true]
#message:yellow:You already completed this quest!
[else]
#message:red:Requirements not met. Need permission, level 10+, and quest not completed.
[endbranch]
```

**Notes:**

- `[OR]` is the default behavior, so `[CONDSTART]` without `[AND]` or `[OR]` will use OR logic
- You can mix AND/OR chains with regular if/else branches
- Empty condition chains (no `[if:...]` lines) will evaluate to false
- The condition chain must have at least one `[if:...]` to be meaningful

### 3.2 Supported Condition Types

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

### 13.1 Multiple If/Else Conditions - Player Profile Check

Check multiple conditions on stored JSON data:

```text
+playerJSON~:{"name": "Steve", "level": 5}
[branch]
[if:var:playerJSON.name:!=]
[if:var:playerJSON.level:!=]
#message:gold:Welcome back %var:playerJSON.name%, level %var:playerJSON.level%!
[else if:var:playerJSON.name:!=]
#message:yellow:Welcome back %var:playerJSON.name%! Complete the tutorial to set your level.
[else]
#message:red:New player! Please set up your profile.
[endbranch]
```

### 13.2 Permission-Based Commands with Multiple Branches

```text
[branch]
[if:permission:server.admin]
#message:red,BOLD:Admin commands available
!give %player% diamond 64
[else if:permission:server.vip]
#message:gold:VIP rewards!
!give %player% gold_ingot 32
[else if:permission:server.member]
#message:green:Member gift
!give %player% iron_ingot 16
[else]
#message:gray:Join our community for rewards!
[endbranch]
```

### 13.3 Health & Level Based Actions

```text
[branch]
[if:health:>15]
[if:level:>=10]
#message:green:You're strong and experienced!
!effect give %player% minecraft:strength 60 1
[else if:health:>15]
#message:yellow:Healthy but inexperienced. Keep playing!
[else if:level:>=10]
#message:gold:Experienced but low health. Be careful!
!effect give %player% minecraft:regeneration 30 1
[else]
#message:red:Low health and level. Here's some help!
!give %player% cooked_beef 16
!effect give %player% minecraft:regeneration 60 2
[endbranch]
```

### 13.4 Delayed, Console, Variable, and Message

```text
[delay:3]+welcomeMsg:Welcome %player%!
[delay:3]#message:green:%var:welcomeMsg%
[delay:4]!give %player% bread 16
```

### 13.5 Webhook + Variable + Condition

```text
      - '%http://localhost:8080/api>>playerJSON::Content-Type:application/json::{"uuid":"%uuid%"}' #or +playerJSON~:{"name": "Steve", "level": 5}'
      - '[delay:2]' #wait for webhook response
      - '[branch]' 
      - '[CONDSTART][AND][if:var:playerJSON.name:!=][if:var:playerJSON.level:!=][CONDEND]' 
      - '#message:gold:Welcome back %var:playerJSON.name%, level %var:playerJSON.level%!'
      - '[else if:var:playerJSON.name:!=]'
      - '#message:yellow:Welcome back %var:playerJSON.name%! (No level data)'
      - '[else]'
      - '#message:red:New player detected!'
      - '[endbranch]'
```

### 13.6 Host Command + Loop

```text
$ls -1 world/playerdata >>playerFiles
[foreach:%var:playerFiles%:file]#message:gray:Found data file: %file%
```

### 13.7 AND Logic - Multiple Requirements

Check that **all** conditions are met before granting access:

```text
[CONDSTART][AND][if:permission:quest.access][if:level:>=20][if:health:>15][if:world:dungeon][CONDEND]
#message:green:All requirements met! Entering hard mode dungeon.
!tp %player% -100 64 -100
!effect give %player% minecraft:resistance 300 1
```

### 13.8 OR Logic - Any Permission Match

Grant rewards if player has **any** of several VIP tiers:

```text
[CONDSTART][OR][if:permission:vip.bronze][if:permission:vip.silver][if:permission:vip.gold][if:permission:vip.platinum][CONDEND]
#message:gold:VIP member detected! Daily reward granted.
!give %player% diamond 5
!give %player% emerald 10
```

### 13.9 Combined AND/OR with Branch

Using AND logic within a branch structure:

```text
[branch]
[CONDSTART][AND][if:permission:event.participate][if:level:>=15][if:var:event_joined:false][CONDEND]
#message:aqua:Joining the event!
+event_joined:true
!tp %player% 0 100 0
[else if:var:event_joined:true]
#message:yellow:You're already in the event!
[else]
#message:red:Requirements not met. Need permission, level 15+, and not already joined.
[endbranch]
```

### 13.10 Complex Multi-Condition Check

Combining multiple AND checks with different actions:

```text
[CONDSTART][AND][if:world:survival][if:gamemode:SURVIVAL][if:health:>10][CONDEND]
#message:green:Survival mode active - teleporting to safe zone
!tp %player% spawn

[CONDSTART][AND][if:world:creative][if:gamemode:CREATIVE][CONDEND]
#message:aqua:Creative mode active - granting builder tools
!give %player% worldedit:wand 1
```

---

## 14. Important Notes

### 14.1 Branch Blocks vs Inline Conditions

**For multi-line conditions, always use `[branch]...[endbranch]`:**

```text
# CORRECT - Multi-line with branch block
[branch]
[if:var:name:!=]
#message:gold:Hello %var:name%!
[else]
#message:yellow:Hello stranger!
[endbranch]

# ALSO WORKS - Single line (no branch needed)
[if:var:name:!=] #message:gold:Hello %var:name%!
```

Without `[branch]...[endbranch]`, multi-line if/else chains may not work correctly.

### 14.2 Variable Suppression

Use `~` suffix to suppress "Variable set" messages:

```text
+playerJSON~:{"name":"Steve"}
```

This stores the variable silently without showing feedback to the player.
