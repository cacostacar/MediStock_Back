package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.AlertaResponse;

import java.util.List;

public interface FonteAlertasEstoque {

    List<AlertaResponse> listar();
}
