package com.carebridge.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "residents")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String roomNumber;

    @Column
    private Date dateOfBirth;

    @ElementCollection
    @CollectionTable(name = "resident_medical_conditions", joinColumns = @JoinColumn(name = "resident_id"))
    @Column(name = "condition")
    private List<String> medicalConditions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "journal_id")
    private Journal journal;
}
