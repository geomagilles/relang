# Reference Template

Use this template for information-oriented, technical documentation.

```markdown
# [Feature Name]

[One sentence definition]

## Syntax

```relang
[Formal syntax]
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `param1` | `Type` | Yes | Description |
| `param2` | `Type` | No | Description (default: `value`) |

## Return Value

Returns `Type`. [Description of what it represents]

## Behavior

[Bullet points of key behaviors]

- Behavior 1
- Behavior 2
- Behavior 3

## Examples

### Basic Usage

```relang
[Simple example]
```

### [Specific Use Case]

```relang
[Example for that case]
```

## Edge Cases

| Condition | Behavior |
|-----------|----------|
| [Condition 1] | [What happens] |
| [Condition 2] | [What happens] |

## Errors

| Error Type | Cause | Resolution |
|------------|-------|------------|
| `ErrorType1` | [When this occurs] | [How to handle] |
| `ErrorType2` | [When this occurs] | [How to handle] |

## Related

- [Related feature 1](reference-related1.md)
- [Related feature 2](reference-related2.md)
- [Explanation of underlying concept](explanation-concept.md)

## Writing Tips

- Use tables for parameters and options
- Be exhaustive; document everything
- Use consistent format across all reference pages
- Link to explanations for "why" questions
- Include type signatures
