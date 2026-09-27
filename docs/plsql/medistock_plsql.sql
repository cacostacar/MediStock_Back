--1. Limpeza
BEGIN
FOR obj IN (
        SELECT object_name, object_type FROM user_objects
         WHERE object_name IN (
             'PRC_REGISTRAR_ALERTAS_CRITICOS', 'PRC_RELATORIO_CONSUMO_HOSPITAL',
             'FN_DIAS_COBERTURA_ESTOQUE', 'FN_STATUS_ESTOQUE_FORMATADO'
         )
    ) LOOP
        EXECUTE IMMEDIATE 'DROP ' || obj.object_type || ' ' || obj.object_name;
END LOOP;

FOR tab IN (
        SELECT table_name FROM user_tables
         WHERE table_name IN ('ALERTAS', 'TRANSFERENCIAS', 'HISTORICO_CONSUMO',
                               'ITENS_ESTOQUE', 'HOSPITAIS')
    ) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || tab.table_name || ' CASCADE CONSTRAINTS';
END LOOP;
END;
/


-- 2. DDL

CREATE TABLE hospitais (
                           id          NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           nome        VARCHAR2(150)   NOT NULL,
                           endereco    VARCHAR2(200),
                           cidade      VARCHAR2(80),
                           estado      VARCHAR2(2),
                           latitude    NUMBER          NOT NULL,
                           longitude   NUMBER          NOT NULL
);

CREATE TABLE itens_estoque (
                               id                        NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               nome                      VARCHAR2(150)   NOT NULL,
                               quantidade_atual          NUMBER          NOT NULL,
                               quantidade_minima         NUMBER          NOT NULL,
                               unidade_medida            VARCHAR2(20),
                               local_armazenamento       VARCHAR2(150),
                               hospital_id               NUMBER          NOT NULL,
                               validade                  DATE,
                               custo_unitario            NUMBER(12,2),
                               alto_custo_baixa_demanda  CHAR(1)         DEFAULT 'N' NOT NULL
                                   CHECK (alto_custo_baixa_demanda IN ('S','N')),
                               atualizado_em             TIMESTAMP       DEFAULT SYSTIMESTAMP,
                               CONSTRAINT fk_item_hospital FOREIGN KEY (hospital_id) REFERENCES hospitais(id)
);

CREATE TABLE historico_consumo (
                                   id                    NUMBER  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   item_estoque_id       NUMBER  NOT NULL,
                                   hospital_id           NUMBER  NOT NULL,
                                   mes_referencia        DATE    NOT NULL,
                                   quantidade_consumida  NUMBER  NOT NULL,
                                   CONSTRAINT fk_hist_item FOREIGN KEY (item_estoque_id) REFERENCES itens_estoque(id),
                                   CONSTRAINT fk_hist_hosp FOREIGN KEY (hospital_id)     REFERENCES hospitais(id)
);

CREATE TABLE transferencias (
                                id                    NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                item_estoque_id       NUMBER          NOT NULL,
                                hospital_origem_id    NUMBER          NOT NULL,
                                hospital_destino_id   NUMBER          NOT NULL,
                                quantidade            NUMBER          NOT NULL,
                                status                VARCHAR2(20)    DEFAULT 'PENDENTE' NOT NULL
                          CHECK (status IN ('PENDENTE','EM_ROTA','CONCLUIDA','CANCELADA')),
                                distancia_km          NUMBER,
                                tempo_estimado_min    NUMBER,
                                motivo                VARCHAR2(300),
                                gerado_por_ia         CHAR(1)         DEFAULT 'N' NOT NULL CHECK (gerado_por_ia IN ('S','N')),
                                criado_em             TIMESTAMP       DEFAULT SYSTIMESTAMP,
                                CONSTRAINT fk_transf_item    FOREIGN KEY (item_estoque_id)     REFERENCES itens_estoque(id),
                                CONSTRAINT fk_transf_origem  FOREIGN KEY (hospital_origem_id)  REFERENCES hospitais(id),
                                CONSTRAINT fk_transf_destino FOREIGN KEY (hospital_destino_id) REFERENCES hospitais(id)
);

-- Tabela nova
CREATE TABLE alertas (
                         id               NUMBER          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         item_estoque_id  NUMBER,
                         hospital_id      NUMBER,
                         tipo             VARCHAR2(20)    NOT NULL CHECK (tipo IN ('CRITICO','ATENCAO','INFO')),
                         mensagem         VARCHAR2(400)   NOT NULL,
                         origem           VARCHAR2(150),
                         criado_em        TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
                         CONSTRAINT fk_alerta_item FOREIGN KEY (item_estoque_id) REFERENCES itens_estoque(id),
                         CONSTRAINT fk_alerta_hosp FOREIGN KEY (hospital_id)     REFERENCES hospitais(id)
);



-- 3. Dados de exemplo (não sei se está certo, pq o claude me deu uns exemplos kkkkkkk)


INSERT INTO hospitais (nome, endereco, cidade, estado, latitude, longitude) VALUES
    ('Hospital das Clinicas', 'Av. Dr. Eneas de Carvalho Aguiar, 255', 'Sao Paulo', 'SP', -23.5570, -46.6695);
INSERT INTO hospitais (nome, endereco, cidade, estado, latitude, longitude) VALUES
    ('Hospital Regional do ABC', 'Av. Industrial, 3100', 'Santo Andre', 'SP', -23.6644, -46.5383);

-- Itens do Hospital 1
INSERT INTO itens_estoque (nome, quantidade_atual, quantidade_minima, unidade_medida, local_armazenamento, hospital_id, validade, custo_unitario)
VALUES ('Dipirona 500mg', 15, 50, 'cx', 'Almoxarifado A - Prateleira 3', 1, DATE '2026-11-15', 12.50);
INSERT INTO itens_estoque (nome, quantidade_atual, quantidade_minima, unidade_medida, local_armazenamento, hospital_id, validade, custo_unitario)
VALUES ('Soro Fisiologico 500ml', 480, 200, 'un', 'Almoxarifado A - Prateleira 1', 1, DATE '2027-06-01', 4.30);
INSERT INTO itens_estoque (nome, quantidade_atual, quantidade_minima, unidade_medida, local_armazenamento, hospital_id, validade, custo_unitario)
VALUES ('Luvas Latex M', 90, 100, 'cx', 'Almoxarifado B - Prateleira 2', 1, DATE '2026-10-10', 22.00);

-- Itens do Hospital 2
INSERT INTO itens_estoque (nome, quantidade_atual, quantidade_minima, unidade_medida, local_armazenamento, hospital_id, validade, custo_unitario)
VALUES ('Paracetamol 750mg', 300, 120, 'cx', 'Farmacia Central', 2, DATE '2027-01-20', 9.90);
INSERT INTO itens_estoque (nome, quantidade_atual, quantidade_minima, unidade_medida, local_armazenamento, hospital_id, validade, custo_unitario)
VALUES ('Insulina NPH', 8, 10, 'un', 'Camara Fria', 2, DATE '2026-10-05', 45.00);

-- Historico de consumo (ultimos 3 meses, referenciado no primeiro dia do mes)
INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (1, 1, ADD_MONTHS(TRUNC(SYSDATE,'MM'), -2), 40);
INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (1, 1, ADD_MONTHS(TRUNC(SYSDATE,'MM'), -1), 55);
INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (1, 1, TRUNC(SYSDATE,'MM'), 60);

INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (2, 1, ADD_MONTHS(TRUNC(SYSDATE,'MM'), -1), 300);
INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (2, 1, TRUNC(SYSDATE,'MM'), 250);

INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (4, 2, TRUNC(SYSDATE,'MM'), 150);
INSERT INTO historico_consumo (item_estoque_id, hospital_id, mes_referencia, quantidade_consumida) VALUES (5, 2, TRUNC(SYSDATE,'MM'), 12);

INSERT INTO transferencias (item_estoque_id, hospital_origem_id, hospital_destino_id, quantidade, status, distancia_km, tempo_estimado_min, motivo, gerado_por_ia)
VALUES (1, 2, 1, 40, 'PENDENTE', 18.4, 32, 'Reposicao automatica sugerida por nivel critico', 'S');

COMMIT;



-- 4.Functions
-- FUNCTION fn_dias_cobertura_estoque
--
-- Proposito:
--   Indicador de negocio (KPI) que estima em quantos DIAS o estoque atual de um item se esgotara, com base no consumo medio mensal registrado em Historico_Consumo nos ultimos 6 meses. E o principal sinal para antecipar
--   rupturas de estoque antes que o nivel minimo seja atingido.
-- Parametro (IN):  p_item_id - identificador do item em ITENS_ESTOQUE
-- Retorno:
--   NUMBER - dias estimados de cobertura, arredondado a 1 casa decimal
--   Retorna NULL quando nao ha historico de consumo suficiente para estimar
--   (nesse caso a recomendacao e olhar apenas o nivel minimo/atual).
-- Excecoes tratadas:
--   NO_DATA_FOUND - item inexistente -> erro de aplicacao (-20001)
--   ZERO_DIVIDE   - consumo medio igual a zero -> retorna NULL (nao e erro)
--   OTHERS        - qualquer outra falha -> erro de aplicacao (-20099)

CREATE OR REPLACE FUNCTION fn_dias_cobertura_estoque (
    p_item_id IN itens_estoque.id%TYPE
) RETURN NUMBER
IS
    v_quantidade_atual   itens_estoque.quantidade_atual%TYPE;
    v_consumo_medio_mes  NUMBER;
    v_dias_cobertura     NUMBER;
BEGIN
-- Garante que o item existe e recupera a quantidade atual
SELECT quantidade_atual
INTO v_quantidade_atual
FROM itens_estoque
WHERE id = p_item_id;

-- Consumo medio mensal nos ultimos 6 meses (0 se nao houver historico)
SELECT NVL(AVG(quantidade_consumida), 0)
INTO v_consumo_medio_mes
FROM historico_consumo
WHERE item_estoque_id = p_item_id
  AND mes_referencia >= ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -6);

IF v_consumo_medio_mes = 0 THEN
        RETURN NULL;
END IF;

    v_dias_cobertura := ROUND(v_quantidade_atual / (v_consumo_medio_mes / 30), 1);
RETURN v_dias_cobertura;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20001, 'Item de estoque ' || p_item_id || ' nao encontrado.');
WHEN ZERO_DIVIDE THEN
        RETURN NULL;
WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20099, 'Erro ao calcular dias de cobertura do item ' ||
                                          p_item_id || ': ' || SQLERRM);
END fn_dias_cobertura_estoque;
/



-- FUNCTION fn_status_estoque_formatado

CREATE OR REPLACE FUNCTION fn_status_estoque_formatado (
    p_item_id IN itens_estoque.id%TYPE
) RETURN VARCHAR2
IS
    v_nome        itens_estoque.nome%TYPE;
    v_qtd_atual   itens_estoque.quantidade_atual%TYPE;
    v_qtd_minima  itens_estoque.quantidade_minima%TYPE;
    v_unidade     itens_estoque.unidade_medida%TYPE;
    v_validade    itens_estoque.validade%TYPE;
    v_hospital    hospitais.nome%TYPE;
    v_nivel       VARCHAR2(10);
    v_dias_venc   NUMBER;
    v_resultado   VARCHAR2(400);
BEGIN
SELECT ie.nome, ie.quantidade_atual, ie.quantidade_minima,
       NVL(ie.unidade_medida, 'un.'), ie.validade, h.nome
INTO v_nome, v_qtd_atual, v_qtd_minima, v_unidade, v_validade, v_hospital
FROM itens_estoque ie
         JOIN hospitais h ON h.id = ie.hospital_id
WHERE ie.id = p_item_id;

-- Mesma regra de negocio de NivelEstoque usada na camada de aplicacao
IF v_qtd_atual <= v_qtd_minima THEN
        v_nivel := 'CRITICO';
    ELSIF v_qtd_atual <= v_qtd_minima * 1.5 THEN
        v_nivel := 'ATENCAO';
ELSE
        v_nivel := 'NORMAL';
END IF;

    v_resultado := '[' || v_nivel || '] ' || v_nome || ' (' || v_hospital || ') - ' ||
                   v_qtd_atual || ' ' || v_unidade || ' (min. ' || v_qtd_minima || ')';

    IF v_validade IS NOT NULL THEN
        v_dias_venc := TRUNC(v_validade) - TRUNC(SYSDATE);
        IF v_dias_venc < 0 THEN
            v_resultado := v_resultado || ' - VENCIDO em ' || TO_CHAR(v_validade, 'DD/MM/YYYY');
        ELSIF v_dias_venc <= 60 THEN
            v_resultado := v_resultado || ' - vence em ' || v_dias_venc || ' dia(s)';
END IF;
END IF;

RETURN v_resultado;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'Item de estoque ' || p_item_id || ' nao encontrado.';
WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20098, 'Erro ao formatar status do item ' ||
                                          p_item_id || ': ' || SQLERRM);
END fn_status_estoque_formatado;
/


-- 5. Procedures
CREATE OR REPLACE PROCEDURE prc_registrar_alertas_criticos (
    p_hospital_id IN itens_estoque.hospital_id%TYPE DEFAULT NULL
)
IS
    -- Cursor com todos os itens do escopo informado (ou todos, se NULL)
    CURSOR c_itens IS
SELECT ie.id, ie.nome, ie.quantidade_atual, ie.quantidade_minima,
       ie.validade, ie.hospital_id, ie.local_armazenamento
FROM itens_estoque ie
WHERE p_hospital_id IS NULL OR ie.hospital_id = p_hospital_id;

v_nivel          VARCHAR2(10);
    v_dias_venc      NUMBER;
    v_ja_registrado  NUMBER;
    v_total_alertas  PLS_INTEGER := 0;
BEGIN
FOR r_item IN c_itens LOOP
BEGIN

            IF r_item.quantidade_atual <= r_item.quantidade_minima THEN
                v_nivel := 'CRITICO';
            ELSIF r_item.quantidade_atual <= r_item.quantidade_minima * 1.5 THEN
                v_nivel := 'ATENCAO';
ELSE
                v_nivel := NULL;
END IF;

            IF v_nivel IS NOT NULL THEN
SELECT COUNT(*) INTO v_ja_registrado
FROM alertas
WHERE item_estoque_id = r_item.id
  AND tipo = v_nivel
  AND mensagem LIKE 'Estoque%'
  AND TRUNC(criado_em) = TRUNC(SYSDATE);

IF v_ja_registrado = 0 THEN
                    INSERT INTO alertas (item_estoque_id, hospital_id, tipo, mensagem, origem)
                    VALUES (r_item.id, r_item.hospital_id, v_nivel,
                            'Estoque ' || LOWER(v_nivel) || ': ' || r_item.quantidade_atual ||
                            ' un. (minimo ' || r_item.quantidade_minima || ') - ' || r_item.nome,
                            r_item.local_armazenamento);
                    v_total_alertas := v_total_alertas + 1;
END IF;
END IF;


            IF r_item.validade IS NOT NULL THEN
                v_dias_venc := TRUNC(r_item.validade) - TRUNC(SYSDATE);

                IF v_dias_venc < 0 THEN
SELECT COUNT(*) INTO v_ja_registrado
FROM alertas
WHERE item_estoque_id = r_item.id
  AND mensagem LIKE 'Item vencido%'
  AND TRUNC(criado_em) = TRUNC(SYSDATE);

IF v_ja_registrado = 0 THEN
                        INSERT INTO alertas (item_estoque_id, hospital_id, tipo, mensagem, origem)
                        VALUES (r_item.id, r_item.hospital_id, 'CRITICO',
                                'Item vencido em ' || TO_CHAR(r_item.validade, 'DD/MM/YYYY') ||
                                ' - ' || r_item.nome,
                                r_item.local_armazenamento);
                        v_total_alertas := v_total_alertas + 1;
END IF;

                ELSIF v_dias_venc <= 60 THEN
SELECT COUNT(*) INTO v_ja_registrado
FROM alertas
WHERE item_estoque_id = r_item.id
  AND mensagem LIKE 'Validade proxima%'
  AND TRUNC(criado_em) = TRUNC(SYSDATE);

IF v_ja_registrado = 0 THEN
                        INSERT INTO alertas (item_estoque_id, hospital_id, tipo, mensagem, origem)
                        VALUES (r_item.id, r_item.hospital_id,
                                CASE WHEN v_dias_venc <= 15 THEN 'ATENCAO' ELSE 'INFO' END,
                                'Validade proxima: vence em ' || v_dias_venc || ' dia(s) - ' ||
                                r_item.nome,
                                r_item.local_armazenamento);
                        v_total_alertas := v_total_alertas + 1;
END IF;
END IF;
END IF;

EXCEPTION

            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Falha ao processar item ' || r_item.id || ': ' || SQLERRM);
END;
END LOOP;

COMMIT;
DBMS_OUTPUT.PUT_LINE(v_total_alertas || ' alerta(s) novo(s) registrado(s).');

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20010, 'Erro ao registrar alertas criticos: ' || SQLERRM);
END prc_registrar_alertas_criticos;
/


-- PROCEDURE prc_relatorio_consumo_hospital

    p_hospital_id      IN  hospitais.id%TYPE,
    p_mes_referencia   IN  DATE,
    p_total_itens      OUT NUMBER,
    p_total_consumido  OUT NUMBER,
    p_cursor           OUT SYS_REFCURSOR
)
IS
    v_existe NUMBER;
BEGIN
SELECT COUNT(*) INTO v_existe FROM hospitais WHERE id = p_hospital_id;
IF v_existe = 0 THEN
        RAISE_APPLICATION_ERROR(-20020, 'Hospital ' || p_hospital_id || ' nao encontrado.');
END IF;

SELECT COUNT(DISTINCT hc.item_estoque_id), NVL(SUM(hc.quantidade_consumida), 0)
INTO p_total_itens, p_total_consumido
FROM historico_consumo hc
WHERE hc.hospital_id = p_hospital_id
  AND TRUNC(hc.mes_referencia, 'MM') = TRUNC(p_mes_referencia, 'MM');

OPEN p_cursor FOR
SELECT ie.nome                                                       AS item,
       hc.quantidade_consumida                                       AS quantidade,
       NVL(ie.unidade_medida, 'un.')                                 AS unidade,
       ie.custo_unitario                                             AS custo_unitario,
       ROUND(hc.quantidade_consumida * NVL(ie.custo_unitario, 0), 2) AS custo_total
FROM historico_consumo hc
         JOIN itens_estoque ie ON ie.id = hc.item_estoque_id
WHERE hc.hospital_id = p_hospital_id
  AND TRUNC(hc.mes_referencia, 'MM') = TRUNC(p_mes_referencia, 'MM')
ORDER BY hc.quantidade_consumida DESC;

EXCEPTION
    WHEN OTHERS THEN
        IF p_cursor%ISOPEN THEN
            CLOSE p_cursor;
END IF;
        RAISE_APPLICATION_ERROR(-20021, 'Erro ao gerar relatorio de consumo do hospital ' ||
                                          p_hospital_id || ': ' || SQLERRM);
END prc_relatorio_consumo_hospital;
/


-- 6. Exemplos


SET SERVEROUTPUT ON;


SELECT id,
       nome,
       fn_status_estoque_formatado(id) AS status_formatado
FROM itens_estoque
ORDER BY id;

-- 6.2) Function usada como criterio de filtro (WHERE) - itens com cobertura curta
SELECT nome, quantidade_atual, fn_dias_cobertura_estoque(id) AS dias_cobertura
FROM itens_estoque
WHERE NVL(fn_dias_cobertura_estoque(id), 999) < 30
ORDER BY dias_cobertura;


BEGIN
    prc_registrar_alertas_criticos;
END;
/


SELECT id, tipo, mensagem, origem, criado_em
FROM alertas
ORDER BY criado_em DESC;


BEGIN
    prc_registrar_alertas_criticos(p_hospital_id => 2);
END;
/

DECLARE
v_total_itens     NUMBER;
    v_total_consumido NUMBER;
    v_cursor          SYS_REFCURSOR;
    v_item            VARCHAR2(150);
    v_qtd             NUMBER;
    v_unidade         VARCHAR2(20);
    v_custo_unit      NUMBER;
    v_custo_total     NUMBER;
BEGIN
    prc_relatorio_consumo_hospital(
        p_hospital_id     => 1,
        p_mes_referencia  => SYSDATE,
        p_total_itens     => v_total_itens,
        p_total_consumido => v_total_consumido,
        p_cursor          => v_cursor
    );

    DBMS_OUTPUT.PUT_LINE('--- Relatorio de consumo - Hospital 1 ---');
    DBMS_OUTPUT.PUT_LINE('Itens distintos consumidos: ' || v_total_itens);
    DBMS_OUTPUT.PUT_LINE('Total de unidades consumidas: ' || v_total_consumido);

    LOOP
FETCH v_cursor INTO v_item, v_qtd, v_unidade, v_custo_unit, v_custo_total;
        EXIT WHEN v_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(' - ' || v_item || ': ' || v_qtd || ' ' || v_unidade ||
                              ' (custo total: R$ ' || v_custo_total || ')');
END LOOP;
CLOSE v_cursor;
END;
/
