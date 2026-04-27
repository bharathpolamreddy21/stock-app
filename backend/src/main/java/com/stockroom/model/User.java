package com.stockroom.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;   // BCrypt-hashed — never stored in plain text

    @Column(nullable = false)
    private String role;       // "ADMIN" or "VIEWER"

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public Long   getId()       { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
    public void   setId(Long id)             { this.id = id; }
    public void   setUsername(String u)      { this.username = u; }
    public void   setPassword(String p)      { this.password = p; }
    public void   setRole(String role)       { this.role = role; }
}
