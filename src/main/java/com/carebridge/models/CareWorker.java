package com.carebridge.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "care_workers")
public class CareWorker extends User {

    @ManyToMany
    @JoinTable(
            name = "careworker_residents",
            joinColumns = @JoinColumn(name = "careworker_id"),
            inverseJoinColumns = @JoinColumn(name = "resident_id")
    )
    private List<Resident> assignedResidents = new ArrayList<>();

    public void addAssignedResident(Resident resident) {
        if (!assignedResidents.contains(resident)) {
            assignedResidents.add(resident);
        }
    }

    public void removeAssignedResident(Resident resident) {
        assignedResidents.remove(resident);
    }

    public List<Resident> getAssignedResidents() {
        return assignedResidents;
    }
}
