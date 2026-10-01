-- =============================================================================
-- MediStock (Smart HAS) - Parte 3: exemplos de uso das functions e procedures
--
-- Execute com F5 como MEDISTOCK / FREEPDB1, depois de 02 (dados simulados)
-- e 06 (objetos da Parte 3). Os exemplos usam os IDs gerados pela carga do
-- script 02 em um schema novo (hospitais 1 a 3, itens 1 a 12).
-- =============================================================================
SET DEFINE OFF
SET SERVEROUTPUT ON
SET LINESIZE 200
SET PAGESIZE 100

BEGIN
    IF SYS_CONTEXT('USERENV', 'SESSION_USER') <> 'MEDISTOCK'
       OR SYS_CONTEXT('USERENV', 'CON_NAME') <> 'FREEPDB1' THEN
        RAISE_APPLICATION_ERROR(-20090,
            'Execute este arquivo na conexao MEDISTOCK do FREEPDB1.');
    END IF;
END;
/


PROMPT 7.1) Function de texto formatado na lista de SELECT
SELECT ID,
       NOME,
       fn_status_estoque_formatado(ID) AS STATUS_FORMATADO
  FROM ITENS_ESTOQUE
 ORDER BY ID;


PROMPT 7.2) Function indicador como filtro (WHERE) e ordenacao: itens que se esgotam em menos de 30 dias
SELECT NOME,
       HOSPITAL_ID,
       QUANTIDADE_ATUAL,
       fn_dias_cobertura_estoque(ID) AS DIAS_COBERTURA
  FROM ITENS_ESTOQUE
 WHERE NVL(fn_dias_cobertura_estoque(ID), 999) < 30
 ORDER BY DIAS_COBERTURA;


PROMPT 7.3) As duas functions juntas em um painel por hospital
SELECT h.NOME                              AS HOSPITAL,
       fn_status_estoque_formatado(ie.ID)  AS SITUACAO,
       fn_dias_cobertura_estoque(ie.ID)    AS DIAS_COBERTURA
  FROM ITENS_ESTOQUE ie
  JOIN HOSPITAIS h ON h.ID = ie.HOSPITAL_ID
 ORDER BY h.NOME, NVL(fn_dias_cobertura_estoque(ie.ID), 999);


PROMPT 7.4) Procedure de alertas para todos os hospitais
DECLARE
    v_total NUMBER;
BEGIN
    prc_registrar_alertas_criticos(p_total_alertas => v_total);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Total retornado: ' || v_total);
END;
/

SELECT ID, TIPO, MENSAGEM, ORIGEM, CRIADO_EM
  FROM ALERTAS
 ORDER BY CRIADO_EM DESC, ID DESC;


PROMPT 7.5) Alertas vigentes (o que a tela de Alertas do app exibe)
SELECT ITEM_NOME, HOSPITAL_NOME, TIPO, MENSAGEM
  FROM vw_alertas_vigentes
 ORDER BY DECODE(TIPO, 'CRITICO', 1, 'ATENCAO', 2, 3), ITEM_NOME;


PROMPT 7.6) Reexecucao restrita ao Hospital 2: nao duplica alertas do mesmo dia (esperado 0)
DECLARE
    v_total NUMBER;
BEGIN
    prc_registrar_alertas_criticos(p_hospital_id => 2, p_total_alertas => v_total);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Alertas novos no Hospital 2: ' || v_total);
END;
/


PROMPT 7.7) Relatorio de consumo do Hospital 1 no mes anterior percorrendo o cursor
DECLARE
    v_total_itens     NUMBER;
    v_total_consumido NUMBER;
    v_custo_total     NUMBER;
    v_cursor          SYS_REFCURSOR;
    v_item            ITENS_ESTOQUE.NOME%TYPE;
    v_qtd             NUMBER;
    v_unidade         ITENS_ESTOQUE.UNIDADE_MEDIDA%TYPE;
    v_custo_unit      NUMBER;
    v_custo_item      NUMBER;
BEGIN
    prc_relatorio_consumo_hospital(
        p_hospital_id     => 1,
        p_mes_referencia  => ADD_MONTHS(SYSDATE, -1),
        p_total_itens     => v_total_itens,
        p_total_consumido => v_total_consumido,
        p_custo_total     => v_custo_total,
        p_cursor          => v_cursor
    );

    DBMS_OUTPUT.PUT_LINE('--- Relatorio de consumo - Hospital 1 - ' ||
                         TO_CHAR(ADD_MONTHS(SYSDATE, -1), 'MM/YYYY') || ' ---');
    DBMS_OUTPUT.PUT_LINE('Itens distintos consumidos: ' || v_total_itens);
    DBMS_OUTPUT.PUT_LINE('Total de unidades consumidas: ' || v_total_consumido);
    DBMS_OUTPUT.PUT_LINE('Custo total: R$ ' || TO_CHAR(v_custo_total, 'FM999G999G990D00'));

    LOOP
        FETCH v_cursor INTO v_item, v_qtd, v_unidade, v_custo_unit, v_custo_item;
        EXIT WHEN v_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(' - ' || v_item || ': ' || v_qtd || ' ' || v_unidade ||
                             ' (custo total: R$ ' || TO_CHAR(v_custo_item, 'FM999G999G990D00') || ')');
    END LOOP;
    CLOSE v_cursor;
END;
/


PROMPT 7.8) Tratamento de erro: hospital inexistente dispara ORA-20120
DECLARE
    v_total_itens     NUMBER;
    v_total_consumido NUMBER;
    v_custo_total     NUMBER;
    v_cursor          SYS_REFCURSOR;
BEGIN
    prc_relatorio_consumo_hospital(9999, SYSDATE, v_total_itens, v_total_consumido, v_custo_total, v_cursor);
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Erro esperado capturado: ' || SQLERRM);
END;
/
