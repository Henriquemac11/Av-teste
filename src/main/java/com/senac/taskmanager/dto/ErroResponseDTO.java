package com.senac.taskmanager.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponseDTO(
        int status,
        String mensagem,
        List<String> erros,
        LocalDateTime timestamp
) {
}
