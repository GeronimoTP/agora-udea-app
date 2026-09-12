package com.udea.agora_backend.repository;

import com.udea.agora_backend.model.ProgramaAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramaAcademicoRepository extends JpaRepository<ProgramaAcademico, Integer> {
    List<ProgramaAcademico> findByFacultadId(Integer facultadId);
}
