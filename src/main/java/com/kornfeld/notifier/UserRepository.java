package com.kornfeld.notifier;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    AppUser findByNameAndPassword(String name, String password);
    AppUser findByName(String name);
    @Query("SELECT (u.name,u.password) FROM AppUser u")
    ArrayList<AppUser> selectAllUsers();

    List<AppUser> findByNameOrEmail(String name, String email);

    List<AppUser> findByToken(String token);
}
