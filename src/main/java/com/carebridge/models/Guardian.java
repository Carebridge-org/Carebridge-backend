package com.carebridge.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "guardians")
public class Guardian extends User {

    @ManyToMany
    @JoinTable(
            name = "guardian_residents",
            joinColumns = @JoinColumn(name = "guardian_id"),
            inverseJoinColumns = @JoinColumn(name = "resident_id")
    )
    private List<Resident> residents = new ArrayList<>();

    // Grønne cirkler = helper metoder
    public void addResident(Resident resident) {
        if (!residents.contains(resident)) {
            residents.add(resident);
        }
    }

    public void removeResident(Resident resident) {
        residents.remove(resident);
    }

    public List<Resident> getResidents() {
        return residents;
    }
}