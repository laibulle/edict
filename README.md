# Edict

Edict is a CLI for generating agent configuration files for different LLM tools,
editors, and coding assistants from one standardized agent file.

## The Problem

AI coding tools are converging on the same basic need: they need project-specific
instructions, conventions, workflows, and tool usage rules. But every product
expects those instructions in a different place and format.

Common examples include:

- repository-level agent instructions
- editor-specific assistant rules
- model-specific system or developer guidance
- CLI assistant configuration files
- prompt fragments copied across multiple tools

This creates a maintenance problem. Teams end up duplicating the same guidance
across several files, such as instructions for architecture, testing, coding
style, commit rules, review expectations, and local commands.

The result is drift:

- one assistant has the latest instructions while another uses stale guidance
- fixes must be copied manually into many files
- each editor or LLM integration becomes a separate source of truth
- onboarding a new tool requires rewriting the same project context again
- teams cannot easily verify which agent instructions are canonical

As more assistants and editors appear, this gets worse. The project knowledge
that should be stable becomes scattered across tool-specific configuration.

## The Solution

Edict uses a single standardized agent file as the source of truth.

Instead of maintaining separate instruction files for every assistant, you define
the project's agent contract once. Edict then generates the target files required
by each supported LLM tool, editor, or coding assistant.

The standardized file captures shared project guidance, such as:

- project purpose and architecture
- coding conventions
- test and build commands
- review expectations
- security and safety rules
- repository-specific workflows
- tool-specific overrides when needed

From that source, the CLI can emit files tailored for different targets while
preserving one canonical instruction set.

## How It Works

The intended workflow is:

1. Write one standardized agent file for the repository.
2. Choose one or more output targets, such as an editor or LLM assistant.
3. Run the Edict CLI.
4. Commit the generated files or use them locally, depending on the team's
   workflow.

Conceptually:

```text
agent.edict.md
   -> AGENTS.md
   -> .cursor/rules/project.md
   -> .github/copilot-instructions.md
   -> tool-specific assistant files
```

The exact target list can grow over time without changing the core authoring
model. Adding support for a new assistant should mean adding a new generator,
not rewriting project guidance.

## Solution Schema

```mermaid
flowchart LR
    author["Developer or Team"] --> source["Standardized Agent File<br/><code>agent.edict.md</code>"]

    source --> parser["Edict Parser<br/>validate and normalize"]
    parser --> model["Canonical Agent Model<br/>shared instructions"]

    model --> shared["Shared Guidance<br/>architecture, style, tests, workflows"]
    model --> overrides["Target Overrides<br/>format and tool-specific rules"]

    shared --> generator["Edict Generators"]
    overrides --> generator

    generator --> agents["<code>AGENTS.md</code>"]
    generator --> cursor["<code>.cursor/rules/project.md</code>"]
    generator --> copilot["<code>.github/copilot-instructions.md</code>"]
    generator --> cli["CLI assistant files"]
    generator --> future["Future LLM and editor targets"]

    agents --> tools["Consistent Agent Behavior"]
    cursor --> tools
    copilot --> tools
    cli --> tools
    future --> tools

    classDef source fill:#f8fafc,stroke:#111827,stroke-width:2px,color:#111827;
    classDef core fill:#e0f2fe,stroke:#0369a1,stroke-width:2px,color:#0c4a6e;
    classDef output fill:#ecfdf5,stroke:#047857,stroke-width:2px,color:#064e3b;
    classDef result fill:#fff7ed,stroke:#c2410c,stroke-width:2px,color:#7c2d12;

    class author,source source;
    class parser,model,shared,overrides,generator core;
    class agents,cursor,copilot,cli,future output;
    class tools result;
```

## Agent Format Specification

The standardized agent file is named `agent.edict.md`.

The public format is Markdown because agent instructions are mostly written and
reviewed by humans. Markdown also makes the format approachable for teams using
any language, editor, or LLM provider.

Edict can still be implemented in Clojure. The implementation detail should not
leak into the authoring experience. The CLI parses Markdown into a canonical
internal model, then generates target-specific files from that model.

### File Shape

An `agent.edict.md` file has two parts:

1. YAML front matter for metadata and target configuration.
2. Markdown sections for the actual agent instructions.

```markdown
---
edict: 1
project:
  name: edict
  summary: Generate agent files for LLM tools and editors from one source file.
targets:
  agents-md:
    path: AGENTS.md
  cursor:
    path: .cursor/rules/project.md
---

# Agent

## Role

Pragmatic coding assistant.

## Behavior

- Read existing code before editing.
- Prefer small, focused changes.
- Verify changes with tests when available.
```

This keeps metadata machine-readable and instructions easy to edit in any
Markdown-capable environment.

### Required Front Matter

#### `edict`

The format version.

```yaml
edict: 1
```

This allows the CLI to evolve the format without silently changing behavior for
older repositories.

#### `project`

Describes the repository the agent is working in.

```yaml
project:
  name: edict
  summary: Generate agent files for LLM tools and editors from one source file.
  languages:
    - Clojure
    - Babashka
  architecture: Small CLI with core generation logic and target adapters.
```

Supported keys:

- `name` - project name
- `summary` - short description of the project
- `languages` - primary languages or runtimes
- `architecture` - concise architecture notes

#### `targets`

Defines output-specific generation settings.

```yaml
targets:
  agents-md:
    enabled: true
    path: AGENTS.md
  cursor:
    enabled: true
    path: .cursor/rules/project.md
    include:
      - project
      - agent
      - commands
      - guidelines
      - workflows
  copilot:
    enabled: true
    path: .github/copilot-instructions.md
    include:
      - project
      - agent
      - guidelines
    overrides:
      tone: Brief and action-oriented inside code review comments.
```

Each target supports:

- `enabled` - whether the target should be generated
- `path` - output path relative to the repository root
- `include` - optional list of sections to include
- `exclude` - optional list of sections to omit
- `overrides` - target-specific values merged into the canonical model

### Required Markdown Sections

#### `# Agent`

Defines the shared behavior expected from generated agent files.

```markdown
# Agent

## Role

Pragmatic coding assistant.

## Tone

Direct, concise, and implementation-focused.

## Behavior

- Read existing code before editing.
- Prefer small, focused changes.
- Follow established project patterns.
- Verify changes with tests when available.
```

Supported subsections:

- `## Role` - the assistant's expected role
- `## Tone` - communication style
- `## Behavior` - baseline working rules

### Optional Markdown Sections

#### `# Commands`

Documents commands the agent should know how to run.

```markdown
# Commands

| Name | Command | Description |
| --- | --- | --- |
| test | `bb test` | Run the test suite. |
| cli | `bb edict-cli` | Run the local CLI. |
```

Each command supports:

- `Name` - stable command identifier
- `Command` - shell command
- `Description` - when to use it
- `Working Directory` - optional relative directory column

#### `# Guidelines`

Defines reusable instruction blocks.

```markdown
# Guidelines

## Code Style

- Follow existing namespace and file organization.
- Keep pure generation logic in core modules.
- Keep CLI argument parsing in CLI modules.

## Testing

- Add focused tests for new generation behavior.
- Run `bb test` before reporting completion.
```

Each second-level heading under `# Guidelines` becomes a reusable guideline
block. The heading text is converted into a stable identifier by the parser.

#### `# Workflows`

Describes common multi-step tasks.

```markdown
# Workflows

## Add a New Target

1. Create a target adapter.
2. Map canonical sections to the target's expected format.
3. Add tests for the generated output.
4. Document the target path and limitations.
```

Each second-level heading under `# Workflows` becomes a named workflow.

#### `# Constraints`

Defines rules the agent should not violate.

```markdown
# Constraints

- Do not overwrite user-authored files without an explicit generation marker.
- Do not introduce network access into generation logic.
- Do not make generated output depend on wall-clock time.
```

#### `# Target Overrides`

Defines instruction text that only applies to one output target.

```markdown
# Target Overrides

## copilot

Keep review comments brief and action-oriented.

## cursor

Prefer instructions that work well inside an editor rules file.
```

### Complete Example

```markdown
---
edict: 1
project:
  name: edict
  summary: Generate agent files for LLM tools and editors from one source file.
  languages:
    - Clojure
    - Babashka
  architecture: A CLI layer parses arguments and delegates deterministic generation to core logic.
targets:
  agents-md:
    enabled: true
    path: AGENTS.md
  cursor:
    enabled: true
    path: .cursor/rules/project.md
    include:
      - project
      - agent
      - commands
      - guidelines
      - workflows
  copilot:
    enabled: true
    path: .github/copilot-instructions.md
    include:
      - project
      - agent
      - guidelines
    overrides:
      tone: Brief and action-oriented inside code review comments.
---

# Agent

## Role

Pragmatic coding assistant.

## Tone

Direct, concise, and implementation-focused.

## Behavior

- Read existing code before editing.
- Prefer small, focused changes.
- Follow established project patterns.
- Verify changes with tests when available.

# Commands

| Name | Command | Description |
| --- | --- | --- |
| test | `bb test` | Run the test suite. |
| cli | `bb edict-cli` | Run the local CLI. |

# Guidelines

## Code Style

- Keep shared generation behavior in core namespaces.
- Keep command-line parsing in CLI namespaces.
- Use deterministic data transformations for generated files.

## Testing

- Add tests for new format behavior.
- Use fixtures for generated output when practical.
- Run `bb test` before reporting completion.

# Workflows

## Add a New Target

1. Create a target adapter.
2. Map canonical sections to the target's expected format.
3. Add tests for the generated output.
4. Document the target path and limitations.

# Constraints

- Generated output must be deterministic.
- The canonical file remains the source of truth.
- Target-specific behavior must live in target configuration or adapters.

# Target Overrides

## copilot

Keep review comments brief and action-oriented.
```

### Validation Rules

The CLI should reject an `agent.edict.md` file when:

- the front matter is not valid YAML
- `edict` is missing or unsupported
- `project` is missing required project metadata
- `# Agent` is missing shared behavior
- a target is enabled without a `path`
- two targets generate to the same path
- `include` or `exclude` references an unknown section
- generated guideline or workflow identifiers are duplicated
- a required table, such as `# Commands`, has missing columns

### Generation Rules

Generated files should be deterministic.

For the same `agent.edict.md` input and CLI version, Edict should produce the
same output every time. Generators should avoid timestamps, random identifiers,
machine-specific paths, and hidden environment-dependent behavior.

Generated files should also include a short header that identifies them as
generated output and points back to `agent.edict.md` as the canonical source.

## Why This Matters

Edict makes agent instructions easier to maintain, review, and trust.

With one source of truth:

- changes to agent behavior are reviewed in one place
- generated files stay consistent across tools
- teams can adopt new assistants without starting from scratch
- tool-specific formats become build artifacts instead of hand-maintained docs
- project guidance becomes portable across editors and LLM providers

The goal is not to force every assistant into the exact same behavior. Some
tools need different formatting, capabilities, or constraints. The goal is to
make shared guidance canonical, then let generators adapt that guidance to each
target's expectations.

## Project Direction

Edict is designed around a small set of ideas:

- a readable standardized agent format
- deterministic generation
- explicit target adapters
- minimal hidden behavior
- clean separation between shared guidance and tool-specific output

This keeps the CLI predictable while still allowing the supported ecosystem of
LLMs, editors, and agent tools to expand.
