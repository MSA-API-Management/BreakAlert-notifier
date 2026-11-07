package net.jamnig.breakalert.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subName;
    private String address;
    private String branch;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    private List<DataChange> dataChanges = new ArrayList<>();
}