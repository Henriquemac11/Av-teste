package com.senac.taskmanager.dto;

import com.senac.taskmanager.model.StatusTarefa;

import java.time.LocalDateTime;

public record TarefaResponseDTO(
        Long id,
        String titulo,
        String descricao,
        StatusTarefa status,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao
) {
}
