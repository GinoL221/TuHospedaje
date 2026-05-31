# Code Review Rules — TuHospedaje

## General
- **REGLA OBLIGATORIA: Ante cualquier cambio de estrategia (una solución no funciona y pensás en un enfoque distinto), PARÁ, comunicale el problema al usuario y preguntale cómo seguir. No tomés decisiones de cambio de estrategia por tu cuenta.**
- **Sin ruido de asistente**: no uses "por supuesto", "claro que sí", "con gusto te ayudo", "muy buena pregunta", "espero que esto te sirva". Hablá directo.
- **Sin paralelismo negativo**: no construyas una idea débil solo para tirarla abajo y poner la tuya ("no se trata de X, se trata de Y", "no es X, es Y"). Decí tu postura directamente.
- **Verbos simples**: preferí "es", "tiene", "usa", "da", "muestra", "cambia", "quita", "agrega". Evitá "sirve como", "funciona como", "actúa como", "representa", "constituye", "ofrece", "apunta a".
- **Sin palabras infladas**: evitá "robusto", "potenciar", "escalable", "optimizar", "sinérgico", "transformador", "disruptivo", "innovador", "intuitivo", "holístico", "empoderar". Usá lenguaje concreto.
- **Chat directo**: sin performance de asistente. Calidez sin rodeos. Hablale a la persona.
- **Sin resumen si el texto es corto**: si el punto ya está claro, no agregues un párrafo final que repite lo mismo.
- **Checklist mental pre-envío**: antes de mandar una respuesta larga, revisá: (1) la primera oración es necesario o es carraspeo?, (2) afirmaciones vagas reemplazables por específicas?, (3) hay importancia falsa?, (4) hay ruido de asistente?, (5) verbos inflados?, (6) paralelismo negativo?, (7) el final suma o repite?
- No hardcoded secrets, credentials, or API keys in source code
- Use environment variables via `.env` for all sensitive values
- Remove debug files, dead code, and unused dependencies before committing
- Never `git push` without asking the user first. Always show what will be pushed and wait for explicit approval

## Java / Spring Boot
- Follow standard Spring layered architecture: Controller → Service (interface + impl) → Repository
- Use DTOs for API responses — never expose JPA entities directly
- Annotate security rules with `@PreAuthorize` — never rely solely on frontend checks
- Use `@Valid` for request validation in controllers
- Prefer constructor injection over `@Autowired` field injection
- Exception handling must go through `GlobalExceptionHandler`

## React / Frontend
- Use functional components with hooks — no class components
- Keep components small and focused (single responsibility)
- API calls go through a dedicated service layer, not directly in components
- Use React Router for navigation, not window.location
- Avoid prop drilling — use composition or context

## Git
- Commit messages follow conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`
- Each commit should be a coherent, deployable unit of work
- Never commit `node_modules`, `dist/`, `target/`, `.env`, or generated files

## Skills

| Skill | Description | File |
|-------|-------------|------|
| `branch-pr` | Create pull requests with issue-first checks | [SKILL.md](~/.config/opencode/skills/branch-pr/SKILL.md) |
| `chained-pr` | Split oversized changes into reviewable stacked PRs | [SKILL.md](~/.config/opencode/skills/chained-pr/SKILL.md) |
| `cognitive-doc-design` | Design docs that reduce cognitive load | [SKILL.md](~/.config/opencode/skills/cognitive-doc-design/SKILL.md) |
| `comment-writer` | Write warm, direct collaboration comments | [SKILL.md](~/.config/opencode/skills/comment-writer/SKILL.md) |
| `go-testing` | Focused Go testing patterns with teatest, golden files | [SKILL.md](~/.config/opencode/skills/go-testing/SKILL.md) |
| `imagegen` | Generate or edit raster images with AI | [SKILL.md](~/.codex/skills/.system/imagegen/SKILL.md) |
| `issue-creation` | Create GitHub issues with issue-first checks | [SKILL.md](~/.config/opencode/skills/issue-creation/SKILL.md) |
| `java-21` | Java 21 modern patterns (records, sealed types, virtual threads) | [SKILL.md](~/.config/opencode/skills/java-21/SKILL.md) |
| `judgment-day` | Blind dual review with adversarial code review | [SKILL.md](~/.config/opencode/skills/judgment-day/SKILL.md) |
| `openai-docs` | Up-to-date OpenAI API documentation and model guidance | [SKILL.md](~/.codex/skills/.system/openai-docs/SKILL.md) |
| `plugin-creator` | Scaffold Codex plugin directories | [SKILL.md](~/.codex/skills/.system/plugin-creator/SKILL.md) |
| `react-19` | React 19 patterns with React Compiler | [SKILL.md](~/.config/opencode/skills/react-19/SKILL.md) |
| `skill-creator` | Create LLM-first skills with valid frontmatter | [SKILL.md](~/.config/opencode/skills/skill-creator/SKILL.md) |
| `skill-improver` | Audit and upgrade existing LLM-first skills | [SKILL.md](~/.config/opencode/skills/skill-improver/SKILL.md) |
| `skill-installer` | Install skills from curated list or GitHub repos | [SKILL.md](~/.codex/skills/.system/skill-installer/SKILL.md) |
| `spring-boot-3` | Spring Boot 3 / JPA / Security 6 + JWT patterns | [SKILL.md](~/.config/opencode/skills/spring-boot-3/SKILL.md) |
| `ui-ux-pro-max` | UI/UX design: 50+ styles, 161 paletas, 57 pares tipográficos, 99 UX guidelines | [SKILL.md](~/.config/opencode/skills/ui-ux-pro-max/SKILL.md) |
| `vercel-react-best-practices` | 70 reglas de performance React/Next.js por Vercel Engineering | [SKILL.md](~/.config/opencode/skills/react-best-practices/SKILL.md) |
| `work-unit-commits` | Plan commits as reviewable work units | [SKILL.md](~/.config/opencode/skills/work-unit-commits/SKILL.md) |
