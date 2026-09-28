package com.senac.taskmanager.repository;

import com.senac.taskmanager.model.StatusTarefa;
import com.senac.taskmanager.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    // usado no cadastro: "ativa" = qualquer status diferente de CONCLUIDA
    boolean existsByTituloIgnoreCaseAndStatusNot(String titulo, StatusTarefa status);

    // usado no update: ignora a propria tarefa que esta sendo editada
    boolean existsByTituloIgnoreCaseAndStatusNotAndIdNot(String titulo, StatusTarefa status, Long id);
}
