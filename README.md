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

É preciso um usuário e senha de um banco Oracle. Há duas opções.

**Opção A: Oracle da FIAP** (usuário `rmXXXXX` e a senha do Oracle de cada aluno)

1. No SQL Developer, conecte em `oracle.fiap.com.br`, porta `1521`, SID `ORCL`, e execute [`docs/plsql/medistock_plsql.sql`](docs/plsql/medistock_plsql.sql) inteiro (F5). Ele cria as tabelas, os dados simulados, a view, as functions e as procedures. Antes, leia o aviso sobre a limpeza em [`docs/plsql/README.md`](docs/plsql/README.md#como-executar-o-script).
2. Adicione ao `.env`:

   ```properties
   SPRING_PROFILES_ACTIVE=oracle
   ORACLE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
   ORACLE_USER=rmXXXXX
   ORACLE_PASSWORD=sua-senha-do-oracle
   ```

3. Rode `./mvnw spring-boot:run`.

**Opção B: Oracle local com Docker** (não precisa de conta)

```bash
docker run -d --name medistock-oracle -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle -e APP_USER=medistock -e APP_USER_PASSWORD=medistock \
  gvenzl/oracle-free:23-slim-faststart

docker cp docs/plsql/medistock_plsql.sql medistock-oracle:/tmp/medistock_plsql.sql
docker exec -it medistock-oracle sqlplus medistock/medistock@localhost/FREEPDB1 @/tmp/medistock_plsql.sql
```

No `.env`:

```properties
SPRING_PROFILES_ACTIVE=oracle
ORACLE_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
ORACLE_USER=medistock
ORACLE_PASSWORD=medistock
```

Para confirmar que subiu no Oracle, procure no log a linha `The following 1 profile is active: "oracle"` e a linha `Rotina de alertas PL/SQL executada`. Nesse modo toda a persistência vai para o Oracle e os endpoints `/api/plsql` aparecem no Swagger.

### Testando a integração com o PL/SQL

Pelo Swagger (`http://localhost:8080/docs`):

1. `POST /api/auth/registrar` com um e-mail `@fiap.com.br` e copie o `token` da resposta.
2. Clique em **Authorize** e cole o token.
3. `PUT /api/estoque/2` deixando o Soro Fisiológico abaixo do mínimo:

   ```json
   {
     "nome": "Soro Fisiologico 500ml",
     "quantidadeAtual": 150,
     "quantidadeMinima": 200,
     "unidadeMedida": "un",
     "localArmazenamento": "Almoxarifado A - Prateleira 1",
     "hospitalId": 1,
     "validade": "2027-06-30",
     "custoUnitario": 4.30,
     "altoCustoBaixaDemanda": false
   }
   ```

4. Confira o resultado:
   - no log: `prc_registrar_alertas_criticos acionada pelo item 2 (hospital 1): 1 alerta(s) novo(s)`;
   - `GET /api/alertas`: aparece "Estoque critico: 150 un (minimo 200)", lido da view `VW_ALERTAS_VIGENTES`;
   - `GET /api/plsql/alertas`: histórico gravado pela procedure na tabela `ALERTAS`.
5. As outras rotas da tag **PL/SQL (Oracle)** executam as functions (`/estoque/indicadores`) e o relatório de consumo (`/relatorios/consumo/1`).

Pelo app mobile, o mesmo fluxo é editar o item na tela **Estoque** e abrir a tela **Alertas**.

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

Ao criar ou atualizar um item de estoque, o back-end publica um `EstoqueAlteradoEvent`. Depois do commit, ele chama a procedure `prc_registrar_alertas_criticos` no Oracle via JDBC, que grava os alertas na tabela `ALERTAS`. A procedure também roda ao iniciar a API e diariamente às 00:05.

A tela Alertas (`GET /api/alertas`) usa uma fonte de alertas de estoque diferente para cada modo:

| Modo | Fonte dos alertas de estoque |
|---|---|
| SQLite (padrão) | Calculados em Java a partir do estoque (`FonteAlertasEstoqueCalculada`) |
| Oracle (profile `oracle`) | Gravados pela procedure, lidos da view `VW_ALERTAS_VIGENTES` (`FonteAlertasEstoqueOracle`) |

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
