package com.kornfeld.notifier.entities;

import com.kornfeld.notifier.entities.DataChange;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subName;
    private String address;
    private String branch;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    private List<DataChange> dataChanges = new ArrayList<>();

    public List<DataChange> getDataChanges() {
        return dataChanges;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
