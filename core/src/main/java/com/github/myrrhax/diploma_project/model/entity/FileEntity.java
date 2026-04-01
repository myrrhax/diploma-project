package com.github.myrrhax.diploma_project.model.entity;

import com.github.myrrhax.diploma_project.model.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "t_files")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_name")
    String originalName;

    @Column(name = "size_bytes")
    long size;

    @Column(name = "media_type")
    String mediaType;

    @Column(name = "storage_provider")
    @Enumerated(EnumType.STRING)
    StorageProvider storageProvider;

    @Column(name = "is_public")
    Boolean isPublic = false;

    @Column(name = "scheme_id")
    UUID schemeId;

    public FileEntity(String originalName, long size, String mediaType, StorageProvider storageProvider, UUID schemeId) {
        this.originalName = originalName;
        this.size = size;
        this.mediaType = mediaType;
        this.storageProvider = storageProvider;

        if (schemeId != null) {
            this.isPublic = false;
            this.schemeId = schemeId;
        } else {
            this.isPublic = true;
        }
    }
}
