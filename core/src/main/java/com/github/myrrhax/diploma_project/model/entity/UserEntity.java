package com.github.myrrhax.diploma_project.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "t_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@NamedEntityGraph(name = "withAuthorities", attributeNodes = { @NamedAttributeNode("authorities") })
public class UserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", nullable = false)
    String password;

    @Column(name = "is_confirmed")
    Boolean isConfirmed;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Builder.Default
    Set<AuthorityEntity> authorities = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "creator")
    @Builder.Default
    Set<SchemeEntity> schemes = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    ConfirmationEntity confirmation;

    public void addConfirmation(ConfirmationEntity confirmation) {
        this.setConfirmation(confirmation);
        confirmation.setUser(this);
    }
}
