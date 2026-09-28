package com.senac.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senac.taskmanager.dto.TarefaRequestDTO;
import com.senac.taskmanager.dto.TarefaResponseDTO;
import com.senac.taskmanager.exception.RecursoNaoEncontradoException;
import com.senac.taskmanager.exception.RegraDeNegocioException;
import com.senac.taskmanager.model.StatusTarefa;
import com.senac.taskmanager.service.TarefaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TarefaController.class)
class TarefaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TarefaService tarefaService;

    private TarefaResponseDTO respostaPadrao(Long id, String titulo, StatusTarefa status) {
        return new TarefaResponseDTO(id, titulo, "Descrição", status, LocalDateTime.now(), null);
    }

    @Test
    @DisplayName("POST /api/tarefas - deve retornar 201 e o JSON da tarefa criada")
    void deveCriarTarefaERetornar201() throws Exception {
        TarefaRequestDTO request = new TarefaRequestDTO("Estudar Spring Boot", "Descrição", null, null);
        when(tarefaService.criar(any(TarefaRequestDTO.class)))
                .thenReturn(respostaPadrao(1L, "Estudar Spring Boot", StatusTarefa.PENDENTE));

        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Estudar Spring Boot"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataCriacao").exists());

        verify(tarefaService, times(1)).criar(any(TarefaRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/tarefas - título curto deve retornar 400 com lista de erros")
    void deveRetornar400QuandoTituloInvalido() throws Exception {
        TarefaRequestDTO request = new TarefaRequestDTO("abc", null, null, null);

        mockMvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros").isArray())
                .andExpect(jsonPath("$.erros[0]").value("titulo: O título deve ter entre 5 e 100 caracteres"));

        verify(tarefaService, never()).criar(any(TarefaRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/tarefas - deve retornar 200 e a lista de tarefas")
    void deveListarTarefas() throws Exception {
        when(tarefaService.listarTodas()).thenReturn(List.of(
                respostaPadrao(1L, "Primeira tarefa", StatusTarefa.PENDENTE),
                respostaPadrao(2L, "Segunda tarefa", StatusTarefa.EM_ANDAMENTO)
        ));

        mockMvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].titulo").value("Primeira tarefa"))
                .andExpect(jsonPath("$[1].status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("GET /api/tarefas/{id} - deve retornar 200 quando existe")
    void deveBuscarTarefaPorId() throws Exception {
        when(tarefaService.buscarPorId(1L))
                .thenReturn(respostaPadrao(1L, "Tarefa existente", StatusTarefa.PENDENTE));

        mockMvc.perform(get("/api/tarefas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Tarefa existente"));
    }

    @Test
    @DisplayName("GET /api/tarefas/{id} - deve retornar 404 quando não existe")
    void deveRetornar404QuandoTarefaNaoExiste() throws Exception {
        when(tarefaService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Tarefa não encontrada com id: 99"));

        mockMvc.perform(get("/api/tarefas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Tarefa não encontrada com id: 99"));
    }

    @Test
    @DisplayName("PUT /api/tarefas/{id} - deve retornar 200 ao atualizar")
    void deveAtualizarTarefa() throws Exception {
        TarefaRequestDTO request = new TarefaRequestDTO("Tarefa atualizada", "Descrição", StatusTarefa.EM_ANDAMENTO, null);
        when(tarefaService.atualizar(eq(1L), any(TarefaRequestDTO.class)))
                .thenReturn(respostaPadrao(1L, "Tarefa atualizada", StatusTarefa.EM_ANDAMENTO));

        mockMvc.perform(put("/api/tarefas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Tarefa atualizada"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("PUT /api/tarefas/{id} - regra de negócio violada deve retornar 400")
    void deveRetornar400QuandoTransicaoInvalida() throws Exception {
        TarefaRequestDTO request = new TarefaRequestDTO("Tarefa pendente", null, StatusTarefa.CONCLUIDA, null);
        when(tarefaService.atualizar(eq(1L), any(TarefaRequestDTO.class)))
                .thenThrow(new RegraDeNegocioException("Não é permitido passar de PENDENTE direto para CONCLUIDA"));

        mockMvc.perform(put("/api/tarefas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é permitido passar de PENDENTE direto para CONCLUIDA"));
    }

    @Test
    @DisplayName("DELETE /api/tarefas/{id} - deve retornar 204")
    void deveExcluirTarefa() throws Exception {
        doNothing().when(tarefaService).excluir(1L);

        mockMvc.perform(delete("/api/tarefas/1"))
                .andExpect(status().isNoContent());

        verify(tarefaService, times(1)).excluir(1L);
    }

    @Test
    @DisplayName("DELETE /api/tarefas/{id} - tarefa concluída deve retornar 400")
    void deveRetornar400AoExcluirTarefaConcluida() throws Exception {
        doThrow(new RegraDeNegocioException("Não é possível excluir uma tarefa já concluída"))
                .when(tarefaService).excluir(1L);

        mockMvc.perform(delete("/api/tarefas/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é possível excluir uma tarefa já concluída"));
    }
}
