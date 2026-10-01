-- Dados totalmente ficticios para um schema de demonstracao vazio.
-- 3 hospitais, 12 itens, 72 historicos, 4 entregas e 4 transferencias.
-- Usuarios de login serao criados pela API, para gerar BCrypt corretamente.
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

BEGIN
    IF SYS_CONTEXT('USERENV', 'SESSION_USER') <> 'MEDISTOCK'
       OR SYS_CONTEXT('USERENV', 'CON_NAME') <> 'FREEPDB1' THEN
        RAISE_APPLICATION_ERROR(-20090,
            'Execute este arquivo na conexao MEDISTOCK do FREEPDB1.');
    END IF;
END;
/

DECLARE
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    v_hospitais t_ids;
    v_itens t_ids;
    v_total NUMBER;
    v_indice PLS_INTEGER := 0;
    v_item PLS_INTEGER;
    v_origem PLS_INTEGER;
    v_destino PLS_INTEGER;
    v_nome VARCHAR2(150);
    v_unidade VARCHAR2(20);
    v_atual NUMBER;
    v_minima NUMBER;
    v_custo NUMBER;
    v_alto BOOLEAN;
    v_status VARCHAR2(20);
    v_validade DATE;
BEGIN
    SELECT (SELECT COUNT(*) FROM USUARIOS)
         + (SELECT COUNT(*) FROM HOSPITAIS)
         + (SELECT COUNT(*) FROM ITENS_ESTOQUE)
         + (SELECT COUNT(*) FROM HISTORICO_CONSUMO)
         + (SELECT COUNT(*) FROM ENTREGAS)
         + (SELECT COUNT(*) FROM TRANSFERENCIAS)
      INTO v_total FROM DUAL;
    IF v_total > 0 THEN
        RAISE_APPLICATION_ERROR(-20092,
            'Ja existem dados. Carga cancelada para evitar duplicacao.');
    END IF;

    INSERT INTO HOSPITAIS (NOME, ENDERECO, CIDADE, ESTADO, LATITUDE, LONGITUDE)
    VALUES ('Hospital Demonstracao A', 'Rua Simulada, 100', 'Belo Horizonte', 'MG', -19.92, -43.94)
    RETURNING ID INTO v_hospitais(1);

    INSERT INTO HOSPITAIS (NOME, ENDERECO, CIDADE, ESTADO, LATITUDE, LONGITUDE)
    VALUES ('Hospital Demonstracao B', 'Avenida Simulada, 200', 'Contagem', 'MG', -19.93, -44.05)
    RETURNING ID INTO v_hospitais(2);

    INSERT INTO HOSPITAIS (NOME, ENDERECO, CIDADE, ESTADO, LATITUDE, LONGITUDE)
    VALUES ('Hospital Demonstracao C', 'Praca Simulada, 300', 'Lagoa Santa', 'MG', -19.63, -43.89)
    RETURNING ID INTO v_hospitais(3);

    FOR h IN 1..3 LOOP
        FOR j IN 1..4 LOOP
            v_indice := v_indice + 1;
            v_alto := FALSE;
            v_unidade := 'UN';
            CASE j
                WHEN 1 THEN
                    v_nome := 'Soro fisiologico 500 ml';
                    v_atual := 15 + h; v_minima := 20; v_custo := 8.50;
                WHEN 2 THEN
                    v_nome := 'Pacote de gaze';
                    v_atual := 28 + h; v_minima := 20; v_custo := 4.20;
                WHEN 3 THEN
                    v_nome := 'Seringa descartavel 10 ml';
                    v_atual := 150 + h; v_minima := 40; v_custo := 1.30;
                WHEN 4 THEN
                    v_nome := 'Kit diagnostico demonstrativo';
                    v_atual := 2 + h; v_minima := 5; v_custo := 1200;
                    v_alto := TRUE;
            END CASE;
            v_validade := TRUNC(SYSDATE) + 180 + h * 10 + j;
            IF h = 1 AND j = 1 THEN v_validade := TRUNC(SYSDATE) + 20; END IF;
            IF h = 1 AND j = 2 THEN v_validade := TRUNC(SYSDATE) - 10; END IF;

            INSERT INTO ITENS_ESTOQUE
                (NOME, QUANTIDADE_ATUAL, QUANTIDADE_MINIMA, UNIDADE_MEDIDA,
                 LOCAL_ARMAZENAMENTO, HOSPITAL_ID, VALIDADE,
                 CUSTO_UNITARIO, ALTO_CUSTO_BAIXA_DEMANDA)
            VALUES
                (v_nome, v_atual, v_minima, v_unidade, 'Almoxarifado demonstracao',
                 v_hospitais(h), v_validade, v_custo, v_alto)
            RETURNING ID INTO v_itens(v_indice);

            FOR m IN 1..6 LOOP
                INSERT INTO HISTORICO_CONSUMO
                    (ITEM_ESTOQUE_ID, HOSPITAL_ID, MES_REFERENCIA, QUANTIDADE_CONSUMIDA)
                VALUES
                    (v_itens(v_indice), v_hospitais(h),
                     ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -m),
                     CASE WHEN j = 4 THEN 1 + MOD(h + m, 3)
                          ELSE 10 * j + 2 * h + m END);
            END LOOP;
        END LOOP;
    END LOOP;

    FOR n IN 1..4 LOOP
        v_origem := MOD(n - 1, 3) + 1;
        v_destino := MOD(v_origem, 3) + 1;
        v_item := (v_origem - 1) * 4 + 1;
        CASE n
            WHEN 1 THEN v_status := 'PENDENTE';
            WHEN 2 THEN v_status := 'EM_ROTA';
            WHEN 3 THEN v_status := 'CONCLUIDA';
            WHEN 4 THEN v_status := 'CANCELADA';
        END CASE;
        INSERT INTO ENTREGAS
            (ITEM_ESTOQUE_ID, HOSPITAL_DESTINO_ID, QUANTIDADE,
             STATUS, DATA_PREVISTA, TRANSPORTADORA)
        VALUES
            (v_itens(v_item), v_hospitais(v_origem), 10 + n, v_status,
             TRUNC(SYSDATE) + n, 'Transportadora Demonstracao');

        INSERT INTO TRANSFERENCIAS
            (ITEM_ESTOQUE_ID, HOSPITAL_ORIGEM_ID, HOSPITAL_DESTINO_ID,
             QUANTIDADE, STATUS, DISTANCIA_KM, TEMPO_ESTIMADO_MIN,
             MOTIVO, GERADO_POR_IA)
        VALUES
            (v_itens(v_item), v_hospitais(v_origem), v_hospitais(v_destino),
             n + 1, v_status, 15 + n * 5, 25 + n * 10,
             'Cenario ficticio para demonstracao academica', FALSE);
    END LOOP;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Carga concluida: 3 hospitais, 12 itens, 72 historicos, 4 entregas, 4 transferencias.');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
