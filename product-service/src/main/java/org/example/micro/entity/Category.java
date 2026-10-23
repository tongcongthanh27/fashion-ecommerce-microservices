package org.example.micro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.micro.entity.BaseEntity;
import org.hibernate.annotations.UuidGenerator;


@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parentId;
}