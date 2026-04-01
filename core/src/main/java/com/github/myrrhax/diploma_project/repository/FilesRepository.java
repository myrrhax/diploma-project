package com.github.myrrhax.diploma_project.repository;

import com.github.myrrhax.diploma_project.model.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FilesRepository extends JpaRepository<FileEntity, UUID> {
}
