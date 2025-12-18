package com.projet.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

	@Id
	private String id;
	private String email;
	private String password; // BCrypt hashed
	private Role role = Role.CITOYEN; // default role

	public User() {}

	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}

	// getters & setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role; }
}
