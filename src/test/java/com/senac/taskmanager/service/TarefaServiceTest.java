package com.senac.taskmanager.service;

import com.senac.taskmanager.dto.TarefaRequestDTO;
import com.senac.taskmanager.dto.TarefaResponseDTO;
import com.senac.taskmanager.exception.RecursoNaoEncontradoException;
import com.senac.taskmanager.exception.RegraDeNegocioException;
import com.senac.taskmanager.model.StatusTarefa;
import com.senac.taskmanager.model.Tarefa;
import com.senac.taskmanager.repository.TarefaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @InjectMocks
    private TarefaService tarefaService;

    // metodo auxiliar pra nao repetir codigo nos testes
    private Tarefa criarTarefa(Long id, String titulo, StatusTarefa status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(id);
        tarefa.setTitulo(titulo);
        tarefa.setDescricao("Descrição de teste");
        tarefa.setStatus(status);
        tarefa.setDataCriacao(LocalDateTime.now());
        return tarefa;
    }

    // ---------------- criar ----------------

    @Test
    @DisplayName("Deve criar tarefa com status PENDENTE e data de criação preenchida")
    void deveCriarTarefaComSucesso() {
        TarefaRequestDTO request = new TarefaRequestDTO("Estudar Spring Boot", "Ver aulas", null, null);

        Mockito.when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNot("Estudar Spring Boot", StatusTarefa.CONCLUIDA))
                .thenReturn(false);
        Mockito.when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocation -> {
            Tarefa t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TarefaResponseDTO resposta = tarefaService.criar(request);

        Assertions.assertEquals(1L, resposta.id());
        Assertions.assertEquals("Estudar Spring Boot", resposta.titulo());
        Assertions.assertEquals(StatusTarefa.PENDENTE, resposta.status());
        Assertions.assertNotNull(resposta.dataCriacao());
        Assertions.assertNull(resposta.dataConclusao());

        ArgumentCaptor<Tarefa> captor = ArgumentCaptor.forClass(Tarefa.class);
        Mockito.verify(tarefaRepository, times(1)).save(captor.capture());
        Assertions.assertEquals("Ver aulas", captor.getValue().getDescricao());
    }

    @Test
    @DisplayName("Não deve criar tarefa com título igual ao de uma tarefa ativa")
    void naoDeveCriarTarefaComTituloDuplicado() {
        TarefaRequestDTO request = new TarefaRequestDTO("Estudar Spring Boot", null, null, null);

        Mockito.when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNot("Estudar Spring Boot", StatusTarefa.CONCLUIDA))
                .thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.criar(request));

        assertTrue(excecao.getMessage().contains("Já existe uma tarefa ativa"));
        Mockito.verify(tarefaRepository, Mockito.never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("Não deve criar tarefa com status diferente de PENDENTE")
    void naoDeveCriarTarefaComStatusDiferenteDePendente() {
        TarefaRequestDTO request = new TarefaRequestDTO("Estudar Spring Boot", null, StatusTarefa.EM_ANDAMENTO, null);

        assertThrows(RegraDeNegocioException.class, () -> tarefaService.criar(request));

        Mockito.verify(tarefaRepository, Mockito.never()).save(any(Tarefa.class));
    }

    // ---------------- consultar ----------------

    @Test
    @DisplayName("Deve listar todas as tarefas")
    void deveListarTodasAsTarefas() {
        List<Tarefa> tarefas = List.of(
                criarTarefa(1L, "Primeira tarefa", StatusTarefa.PENDENTE),
                criarTarefa(2L, "Segunda tarefa", StatusTarefa.EM_ANDAMENTO)
        );
        Mockito.when(tarefaRepository.findAll()).thenReturn(tarefas);

        List<TarefaResponseDTO> resultado = tarefaService.listarTodas();

        assertEquals(2, resultado.size());
        assertEquals("Primeira tarefa", resultado.get(0).titulo());
        assertEquals("Segunda tarefa", resultado.get(1).titulo());
        Mockito.verify(tarefaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar tarefa por id existente")
    void deveBuscarTarefaPorId() {
        Mockito.when(tarefaRepository.findById(1L))
                .thenReturn(Optional.of(criarTarefa(1L, "Tarefa existente", StatusTarefa.PENDENTE)));

        TarefaResponseDTO resposta = tarefaService.buscarPorId(1L);

        assertEquals(1L, resposta.id());
        assertEquals("Tarefa existente", resposta.titulo());
        Mockito.verify(tarefaRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar id inexistente")
    void deveLancarExcecaoQuandoIdNaoExiste() {
        Mockito.when(tarefaRepository.findById(99L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException excecao = assertThrows(RecursoNaoEncontradoException.class,
                () -> tarefaService.buscarPorId(99L));

        assertEquals("Tarefa não encontrada com id: 99", excecao.getMessage());
    }

    // ---------------- atualizar ----------------

    @Test
    @DisplayName("Deve atualizar tarefa de PENDENTE para EM_ANDAMENTO")
    void deveAtualizarTarefaParaEmAndamento() {
        Tarefa existente = criarTarefa(1L, "Tarefa antiga", StatusTarefa.PENDENTE);
        TarefaRequestDTO request = new TarefaRequestDTO("Tarefa nova", "Nova descrição", StatusTarefa.EM_ANDAMENTO, null);

        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));
        Mockito.when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.atualizar(1L, request);

        assertEquals("Tarefa nova", resposta.titulo());
        assertEquals("Nova descrição", resposta.descricao());
        assertEquals(StatusTarefa.EM_ANDAMENTO, resposta.status());
        assertNull(resposta.dataConclusao());
        Mockito.verify(tarefaRepository, times(1)).save(existente);
    }

    @Test
    @DisplayName("Deve preencher dataConclusao ao mudar de EM_ANDAMENTO para CONCLUIDA")
    void devePreencherDataConclusaoAoConcluir() {
        Tarefa existente = criarTarefa(1L, "Tarefa em andamento", StatusTarefa.EM_ANDAMENTO);
        TarefaRequestDTO request = new TarefaRequestDTO("Tarefa em andamento", null, StatusTarefa.CONCLUIDA, null);

        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));
        Mockito.when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponseDTO resposta = tarefaService.atualizar(1L, request);

        assertEquals(StatusTarefa.CONCLUIDA, resposta.status());
        assertNotNull(resposta.dataConclusao());
        Mockito.verify(tarefaRepository, times(1)).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("Não deve permitir PENDENTE direto para CONCLUIDA")
    void naoDevePermitirPendenteParaConcluida() {
        Tarefa existente = criarTarefa(1L, "Tarefa pendente", StatusTarefa.PENDENTE);
        TarefaRequestDTO request = new TarefaRequestDTO("Tarefa pendente", null, StatusTarefa.CONCLUIDA, null);

        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.atualizar(1L, request));

        assertTrue(excecao.getMessage().contains("PENDENTE direto para CONCLUIDA"));
        assertEquals(StatusTarefa.PENDENTE, existente.getStatus());
        Mockito.verify(tarefaRepository, Mockito.never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("Não deve atualizar tarefa com título de outra tarefa ativa")
    void naoDeveAtualizarComTituloDuplicado() {
        Tarefa existente = criarTarefa(1L, "Tarefa original", StatusTarefa.PENDENTE);
        TarefaRequestDTO request = new TarefaRequestDTO("Título repetido", null, null, null);

        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));
        Mockito.when(tarefaRepository.existsByTituloIgnoreCaseAndStatusNotAndIdNot("Título repetido", StatusTarefa.CONCLUIDA, 1L))
                .thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> tarefaService.atualizar(1L, request));

        Mockito.verify(tarefaRepository, Mockito.never()).save(any(Tarefa.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar id inexistente")
    void deveLancarExcecaoAoAtualizarIdInexistente() {
        TarefaRequestDTO request = new TarefaRequestDTO("Qualquer título", null, null, null);
        Mockito.when(tarefaRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> tarefaService.atualizar(50L, request));

        Mockito.verify(tarefaRepository, Mockito.never()).save(any(Tarefa.class));
    }

    // ---------------- excluir ----------------

    @Test
    @DisplayName("Deve excluir tarefa que não está concluída")
    void deveExcluirTarefaNaoConcluida() {
        Tarefa existente = criarTarefa(1L, "Tarefa pendente", StatusTarefa.PENDENTE);
        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));

        tarefaService.excluir(1L);

        Mockito.verify(tarefaRepository, times(1)).delete(existente);
    }

    @Test
    @DisplayName("Não deve excluir tarefa concluída")
    void naoDeveExcluirTarefaConcluida() {
        Tarefa existente = criarTarefa(1L, "Tarefa concluída", StatusTarefa.CONCLUIDA);
        Mockito.when(tarefaRepository.findById(1L)).thenReturn(Optional.of(existente));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> tarefaService.excluir(1L));

        assertEquals("Não é possível excluir uma tarefa já concluída", excecao.getMessage());
        Mockito.verify(tarefaRepository, Mockito.never()).delete(any(Tarefa.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao excluir id inexistente")
    void deveLancarExcecaoAoExcluirIdInexistente() {
        Mockito.when(tarefaRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> tarefaService.excluir(77L));

        Mockito.verify(tarefaRepository, Mockito.never()).delete(any(Tarefa.class));
    }
}
