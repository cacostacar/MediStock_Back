# Pipeline CI/CD — MediStock (Smart HAS)

Pipeline de integração e entrega contínua em **GitHub Actions**, definido em
[.github/workflows/ci-cd.yml](../.github/workflows/ci-cd.yml).

## Gatilhos

| Evento | O que executa |
| --- | --- |
| `push` na branch `main` | Build, testes, integração Oracle, deploy e relatório |
| `pull_request` para `main` | Build, testes e integração Oracle (sem deploy) |
| Execução manual (`workflow_dispatch`) | Tudo, com a opção **carga inicial** para criar tabelas e dados no schema alvo |

## Jobs

### 1. `build` — Build e testes automatizados
JDK 25 (Temurin) com cache do Maven, `./mvnw verify` sobre a suíte de testes
(SQLite em memória, perfil `test`). Publica o `.jar` como artefato e o log
completo do build junto dos relatórios do Surefire.

### 2. `oracle-it` — Integração com Oracle
Sobe um **Oracle Database Free** real como *service container*
(`gvenzl/oracle-free`, serviço `FREEPDB1`, usuário `MEDISTOCK`), aplica os
scripts de [database/oracle](../database/oracle) na ordem
`01 → 02 → 03 → 06 → 04 → 05 → 07` e roda a suíte novamente com as variáveis
`ORACLE_*` preenchidas — o que habilita o teste
`HistoricoConsumoOracleRealDbTest`, anotado com
`@EnabledIfEnvironmentVariable(named = "ORACLE_PASSWORD")`.

Isso valida, a cada commit, que as tabelas, a procedure `PR_REGISTRAR_CONSUMO`
e os objetos PL/SQL da Parte 3 compilam e funcionam contra um banco de verdade.

### 3. `deploy` — Back-end + Oracle
1. Aplica os objetos PL/SQL no **banco Oracle alvo** com o Oracle SQLcl
   (`03`, `06` e a validação `04`; `01` e `02` apenas quando a carga inicial é
   solicitada na execução manual).
2. Constrói a imagem a partir do [Dockerfile](../Dockerfile) e a publica no
   **GitHub Container Registry**, com as tags `:<sha>` e `:latest`.
3. Opcionalmente faz o *rollout* por SSH no servidor, substituindo o container
   em execução pela nova imagem.
4. Executa um **smoke test** HTTP contra a API publicada.

O job usa o *environment* `producao`, o que permite exigir aprovação manual
antes do deploy nas configurações do repositório.

### 4. `relatorio` — Logs e relatórios de execução
Roda sempre (`if: always()`), baixa todos os artefatos e gera o
`relatorio-execucao.md` com a situação de cada job, o resumo dos testes e a
lista de logs publicados. O mesmo conteúdo aparece no resumo da execução, na
aba **Actions**.

## Artefatos gerados a cada execução

| Artefato | Conteúdo |
| --- | --- |
| `medistock-jar` | `.jar` executável da aplicação |
| `relatorios-build` | `logs/build.log`, `logs/relatorio-testes.md`, relatórios XML do Surefire |
| `relatorios-oracle` | `logs/integracao-oracle.log`, `logs/banco/*.log` (saída de cada script SQL), relatórios do Surefire |
| `relatorios-deploy` | `logs/banco/*.log` do banco alvo, `logs/rollout.log`, `logs/smoke-test.log` |
| `relatorio-execucao` | Relatório consolidado em Markdown |

## Secrets do repositório

Cadastrar em **Settings → Secrets and variables → Actions → Secrets**.

| Secret | Obrigatório | Exemplo / observação |
| --- | --- | --- |
| `ORACLE_URL` | Sim | `jdbc:oracle:thin:@//host:1521/FREEPDB1` — Oracle alvo do deploy |
| `ORACLE_USER` | Sim | `MEDISTOCK` |
| `ORACLE_PASSWORD` | Sim | Senha do usuário `MEDISTOCK` |
| `JWT_SECRET` | Sim | Chave Base64 usada pela API em produção |
| `GEMINI_API_KEY` | Não | Deixe vazia para desligar a integração de IA |
| `DEPLOY_SSH_HOST` | Só com rollout SSH | IP ou domínio do servidor |
| `DEPLOY_SSH_USER` | Só com rollout SSH | Usuário com acesso ao Docker no servidor |
| `DEPLOY_SSH_KEY` | Só com rollout SSH | Chave privada OpenSSH completa, incluindo as linhas `BEGIN`/`END` |
| `DEPLOY_SSH_PORT` | Não | Padrão `22` |

`GITHUB_TOKEN` é fornecido automaticamente pelo GitHub e é o que autentica a
publicação da imagem no GHCR — não precisa ser cadastrado.

## Variables do repositório

Cadastrar em **Settings → Secrets and variables → Actions → Variables**.

| Variable | Para que serve |
| --- | --- |
| `DEPLOY_SSH_ENABLED` | `true` liga o rollout por SSH; qualquer outro valor (ou ausente) pula a etapa |
| `DEPLOY_HEALTHCHECK_URL` | URL verificada pelo smoke test, ex.: `http://meu-servidor:8080/v3/api-docs`. Sem ela, o smoke test é pulado |

## Executando sem servidor de deploy

Sem `DEPLOY_SSH_ENABLED` e sem `DEPLOY_HEALTHCHECK_URL`, o pipeline continua
completo e demonstrável: build, testes, provisionamento e testes contra um
Oracle real, aplicação dos objetos PL/SQL no banco alvo, publicação da imagem
versionada no GHCR e relatório consolidado.
