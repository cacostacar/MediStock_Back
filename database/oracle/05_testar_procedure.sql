-- Execute apos 02 e 03, como MEDISTOCK com F5.
-- Insere um historico de teste e desfaz esse registro ao final.
-- A identidade pode saltar um numero, mesmo com ROLLBACK; isso e normal.
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
    v_item NUMBER;
    v_hospital NUMBER;
    v_hospital_inexistente NUMBER;
    v_antes NUMBER;
    v_depois NUMBER;
    v_rejeitou BOOLEAN;
BEGIN
    SAVEPOINT medistock_teste_proc;
    SELECT MIN(ID) INTO v_item FROM ITENS_ESTOQUE;
    IF v_item IS NULL THEN
        RAISE_APPLICATION_ERROR(-20094, 'Execute a carga simulada antes do teste.');
    END IF;
    SELECT HOSPITAL_ID INTO v_hospital FROM ITENS_ESTOQUE WHERE ID = v_item;
    SELECT NVL(MAX(ID), 0) + 1 INTO v_hospital_inexistente FROM HOSPITAIS;
    SELECT COUNT(*) INTO v_antes FROM HISTORICO_CONSUMO;

    PR_REGISTRAR_CONSUMO(v_item, v_hospital, TRUNC(SYSDATE, 'MM'), 7);
    SELECT COUNT(*) INTO v_depois FROM HISTORICO_CONSUMO;
    IF v_depois <> v_antes + 1 THEN
        RAISE_APPLICATION_ERROR(-20095, 'A chamada valida deveria inserir um registro.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('OK: chamada valida inseriu um historico.');

    v_rejeitou := FALSE;
    BEGIN
        PR_REGISTRAR_CONSUMO(v_item, v_hospital, TRUNC(SYSDATE, 'MM'), -1);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE <> -20002 THEN RAISE; END IF;
        v_rejeitou := TRUE;
    END;
    IF NOT v_rejeitou THEN
        RAISE_APPLICATION_ERROR(-20096, 'Quantidade negativa deveria ser rejeitada.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('OK: quantidade negativa rejeitada.');

    v_rejeitou := FALSE;
    BEGIN
        PR_REGISTRAR_CONSUMO(v_item, v_hospital_inexistente, TRUNC(SYSDATE, 'MM'), 7);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE <> -20005 THEN RAISE; END IF;
        v_rejeitou := TRUE;
    END;
    IF NOT v_rejeitou THEN
        RAISE_APPLICATION_ERROR(-20097, 'Hospital inexistente deveria ser rejeitado.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('OK: hospital inexistente rejeitado.');

    ROLLBACK TO medistock_teste_proc;
    SELECT COUNT(*) INTO v_depois FROM HISTORICO_CONSUMO;
    IF v_depois <> v_antes THEN
        RAISE_APPLICATION_ERROR(-20098, 'A procedure nao preservou o controle da transacao.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('OK: ROLLBACK removeu o registro de teste. Contagem original preservada.');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO medistock_teste_proc;
        RAISE;
END;
/
