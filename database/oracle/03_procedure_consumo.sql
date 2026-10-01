-- Execute como MEDISTOCK com F5. Reexecutar atualiza esta procedure.
-- A procedure registra historico. Nao altera o saldo de estoque.
-- Sem COMMIT/ROLLBACK interno: a transacao pertence a quem a chamou.
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

CREATE OR REPLACE PROCEDURE PR_REGISTRAR_CONSUMO (
    p_item_estoque_id IN NUMBER,
    p_hospital_id IN NUMBER,
    p_mes_referencia IN DATE,
    p_quantidade_consumida IN NUMBER
)
AUTHID DEFINER
AS
    v_existe NUMBER;
BEGIN
    IF p_item_estoque_id IS NULL OR p_item_estoque_id <= 0
       OR p_item_estoque_id <> TRUNC(p_item_estoque_id)
       OR p_hospital_id IS NULL OR p_hospital_id <= 0
       OR p_hospital_id <> TRUNC(p_hospital_id) THEN
        RAISE_APPLICATION_ERROR(-20001, 'Informe IDs inteiros positivos.');
    END IF;
    IF p_quantidade_consumida IS NULL OR p_quantidade_consumida < 0
       OR p_quantidade_consumida > 2147483647
       OR p_quantidade_consumida <> TRUNC(p_quantidade_consumida) THEN
        RAISE_APPLICATION_ERROR(-20002, 'Quantidade deve ser um inteiro nao negativo compativel com Java Integer.');
    END IF;
    IF p_mes_referencia IS NULL THEN
        RAISE_APPLICATION_ERROR(-20003, 'Informe a data de referencia.');
    END IF;

    SELECT COUNT(*) INTO v_existe FROM ITENS_ESTOQUE WHERE ID = p_item_estoque_id;
    IF v_existe = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Item de estoque nao encontrado.');
    END IF;
    SELECT COUNT(*) INTO v_existe FROM HOSPITAIS WHERE ID = p_hospital_id;
    IF v_existe = 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'Hospital nao encontrado.');
    END IF;

    INSERT INTO HISTORICO_CONSUMO
        (ITEM_ESTOQUE_ID, HOSPITAL_ID, MES_REFERENCIA, QUANTIDADE_CONSUMIDA)
    VALUES
        (p_item_estoque_id, p_hospital_id, p_mes_referencia, p_quantidade_consumida);
END;
/
SHOW ERRORS PROCEDURE PR_REGISTRAR_CONSUMO

-- Oracle pode criar um objeto INVALID em vez de lancar erro de compilacao.
-- Por isso verificamos o status explicitamente.
DECLARE
    v_validos NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_validos FROM USER_OBJECTS
    WHERE OBJECT_NAME = 'PR_REGISTRAR_CONSUMO'
      AND OBJECT_TYPE = 'PROCEDURE' AND STATUS = 'VALID';
    IF v_validos <> 1 THEN
        RAISE_APPLICATION_ERROR(-20093, 'Procedure invalida: consulte USER_ERRORS.');
    END IF;
END;
/
