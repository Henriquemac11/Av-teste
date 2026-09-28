package com.senac.taskmanager.service;

import com.senac.taskmanager.dto.TarefaRequestDTO;
import com.senac.taskmanager.dto.TarefaResponseDTO;
import com.senac.taskmanager.exception.RecursoNaoEncontradoException;
import com.senac.taskmanager.exception.RegraDeNegocioException;
import com.senac.taskmanager.model.StatusTarefa;
import com.senac.taskmanager.model.Tarefa;
import com.senac.taskmanager.repository.TarefaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TarefaService {

    private final TarefaRepository tarefaRepository;

    public TarefaService(TarefaRepository tarefaRepository) {
        this.tarefaRepository = tarefaRepository;
    }

    public TarefaResponseDTO criar(TarefaRequestDTO dto) {
        // toda tarefa nova comeca como PENDENTE
        if (dto.status() != null && dto.status() != StatusTarefa.PENDENTE) {
            throw new RegraDeNegocioException("Uma nova tarefa só pode ser criada com o status PENDENTE");
        }

        String titulo = dto.titulo().trim();

        if (tarefaRepository.existsByTituloIgnoreCaseAndStatusNot(titulo, StatusTarefa.CONCLUIDA)) {
            throw new RegraDeNegocioException("Já existe uma tarefa ativa com o título: " + titulo);
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(dto.descricao());
        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefa.setDataCriacao(LocalDateTime.now());

        Tarefa salva = tarefaRepository.save(tarefa);
        return paraResponse(salva);
    }

    public List<TarefaResponseDTO> listarTodas() {
        return tarefaRepository.findAll()
                .stream()
                .map(this::paraResponse)
                .toList();
    }

    public TarefaResponseDTO buscarPorId(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    public TarefaResponseDTO atualizar(Long id, TarefaRequestDTO dto) {
        Tarefa tarefa = buscarEntidade(id);

        // se nao mandou status, mantem o que ja estava
        if (dto.status() != null) {
            validarTransicao(tarefa.getStatus(), dto.status());
            tarefa.setStatus(dto.status());
        }

        String titulo = dto.titulo().trim();
        if (tarefaRepository.existsByTituloIgnoreCaseAndStatusNotAndIdNot(titulo, StatusTarefa.CONCLUIDA, id)) {
            throw new RegraDeNegocioException("Já existe uma tarefa ativa com o título: " + titulo);
        }

        tarefa.setTitulo(titulo);
        tarefa.setDescricao(dto.descricao());

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            // se veio data no request usa ela, senao preenche com agora
            if (dto.dataConclusao() != null) {
                tarefa.setDataConclusao(dto.dataConclusao());
            } else if (tarefa.getDataConclusao() == null) {
                tarefa.setDataConclusao(LocalDateTime.now());
            }
        } else {
            tarefa.setDataConclusao(null);
        }

        Tarefa atualizada = tarefaRepository.save(tarefa);
        return paraResponse(atualizada);
    }

    public void excluir(Long id) {
        Tarefa tarefa = buscarEntidade(id);

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException("Não é possível excluir uma tarefa já concluída");
        }

        tarefaRepository.delete(tarefa);
    }

    private Tarefa buscarEntidade(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada com id: " + id));
    }

    private void validarTransicao(StatusTarefa atual, StatusTarefa novo) {
        if (atual == StatusTarefa.PENDENTE && novo == StatusTarefa.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Não é permitido passar de PENDENTE direto para CONCLUIDA. Passe por EM_ANDAMENTO primeiro");
        }
    }

    private TarefaResponseDTO paraResponse(Tarefa tarefa) {
        return new TarefaResponseDTO(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getStatus(),
                tarefa.getDataCriacao(),
                tarefa.getDataConclusao()
        );
    }
}
