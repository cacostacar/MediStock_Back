# MediStock — Back-end (Smart HAS)

API REST do MediStock, sistema de gestão de estoque hospitalar em rede. Ela controla os insumos de cada hospital, gera alertas de estoque e validade, organiza a logística de entregas e transferências e usa IA para prever a demanda e sugerir a redistribuição de insumos de alto custo entre os hospitais.

É consumida pelo app mobile [`HospitalManagement-ReactNative`](../HospitalManagement-ReactNative).

## Stack

- Java 25 + Spring Boot 4 (Web, Data JPA, Security, Validation)
- Autenticação JWT (jjwt) com e-mail institucional
- SQLite (padrão) ou **Oracle** (profile `oracle`, com procedures e functions PL/SQL)
- Google Gemini para as análises de IA
- Swagger / OpenAPI em `/docs`

## Arquitetura

```
controller/   Endpoints REST (um por tela/módulo do app)
service/      Regras de negócio, IA e integração PL/SQL
repository/   Spring Data JPA + PlsqlRepository (JDBC para o Oracle)
model/        Entidades JPA e enums de domínio
dto/          Records de entrada/saída da API
event/        Eventos de domínio (EstoqueAlteradoEvent)
security/     Filtro e utilitários JWT
exception/    Exceções de negócio e tratamento global de erros
validation/   Validação de e-mail institucional
util/         Cálculo geográfico (distância e rota entre hospitais)
```

## Como executar

Pré-requisito: JDK 25.

Crie um arquivo `.env` na raiz:

```properties
JWT_SECRET=uma-chave-com-pelo-menos-32-caracteres
GEMINI_API_KEY=sua-chave-do-gemini
```

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080` com SQLite (`medistock.db`, criado automaticamente). A documentação interativa fica em `http://localhost:8080/docs`.

### Rodando com Oracle

1. Execute [`docs/plsql/medistock_plsql.sql`](docs/plsql/medistock_plsql.sql) no Oracle. Ele cria as tabelas, os dados simulados, as functions e as procedures.
2. Adicione ao `.env`:

   ```properties
   SPRING_PROFILES_ACTIVE=oracle
   ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
   ORACLE_USER=rmXXXXX
   ORACLE_PASSWORD=********
   ```

3. Rode `./mvnw spring-boot:run`.

Nesse modo toda a persistência vai para o Oracle e os endpoints `/api/plsql` ficam disponíveis.

## Endpoints

Todas as rotas, exceto `/api/auth/**` e `/docs`, exigem o header `Authorization: Bearer <token>`.

| Módulo | Rotas principais |
|---|---|
| Autenticação | `POST /api/auth/registrar`, `POST /api/auth/login` |
| Perfil | `GET /api/perfil/matricula` |
| Hospitais | CRUD em `/api/hospitais` |
| Estoque | CRUD em `/api/estoque`, `GET /api/estoque/resumo` |
| Alertas | `GET /api/alertas`, `GET /api/alertas/resumo` |
| Histórico de consumo | `POST /api/historico-consumo` |
| Logística | `/api/logistica/entregas`, `/api/logistica/transferencias`, `GET /api/logistica/mapa` |
| IA | `GET /api/ia/analise-interna`, `GET /api/ia/redistribuicao`, `POST /api/ia/redistribuicao/{id}/confirmar` |
| PL/SQL (profile `oracle`) | `POST /api/plsql/alertas/processar`, `GET /api/plsql/alertas`, `GET /api/plsql/estoque/indicadores`, `GET /api/plsql/relatorios/consumo/{hospitalId}` |

## Regras de negócio

- **Nível do estoque:** `CRITICO` quando a quantidade atual é menor ou igual ao mínimo, `ATENCAO` até 1,5× o mínimo e `NORMAL` acima disso.
- **Validade:** item vencido gera alerta crítico. Vencimento em até 15 dias gera alerta de atenção e em até 60 dias, alerta informativo.
- **Redistribuição por IA:** para insumos de alto custo e baixa demanda, o sistema escolhe o hospital mais central da rede, aquele com a menor distância ponderada pela demanda histórica dos demais hospitais. O Gemini redige a justificativa, e ao confirmar a sugestão é criada uma transferência pendente.

## Camada Oracle / PL/SQL

- **Modelo de dados (DER):** [`docs/database/DER.md`](docs/database/DER.md)
- **Procedures e functions:** [`docs/plsql/README.md`](docs/plsql/README.md)

Ao criar ou atualizar um item de estoque, o back-end publica um `EstoqueAlteradoEvent`. Depois do commit, ele chama a procedure `prc_registrar_alertas_criticos` no Oracle via JDBC, que grava os alertas na tabela `ALERTAS`.

## Tratamento de erros

As respostas de erro seguem um formato único:

```json
{
  "timestamp": "2026-09-30T20:41:11",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Hospital 99 nao encontrado."
}
```

| Situação | Status |
|---|---|
| Validação de campos (retorna `campos` com o erro de cada campo) | 400 |
| Parâmetro inválido ou regra de negócio violada | 400 |
| Credenciais inválidas | 401 |
| Recurso não encontrado (inclusive erros `ORA-20001`/`ORA-20020` do PL/SQL) | 404 |
| E-mail já cadastrado | 409 |
