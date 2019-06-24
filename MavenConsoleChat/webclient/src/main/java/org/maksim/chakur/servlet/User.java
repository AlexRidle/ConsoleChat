package org.maksim.chakur.servlet;

import java.util.Objects;

public class User {
	private String name;
	private String character; //Better to create enum "Role" and name this value as Role.
	private String password;
	private ClientService clientService;
	
	public User() {
		
	}
	
	public User(String name, String password, String character) {
		this.name = name;
		this.password = password;
		this.character = character;
	}
	
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getCharacter() {return character;}
	public void setCharacter(String character){this.character = character;}
	public String getPassword() {return password;}
	public void setPassword(String password) {this.password = password;}
	public void setClientService(ClientService clientService) {this.clientService = clientService;}
	public ClientService getClientService() {return clientService;}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(name, user.name) &&
				Objects.equals(character, user.character) &&
				Objects.equals(password, user.password);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, character, password);
	}
}
