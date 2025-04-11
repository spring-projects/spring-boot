package org.springframework.boot.autoconfigure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.mail")
public class EmailProperties {
	private String host = "smtp.example.com";
	private int port = 587;
	private String username;
	private String password;
	private boolean auth = true;
	private boolean starttls = true;

	// Getters and setters

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isAuth() {
		return auth;
	}
	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	public boolean isStarttls() {
		return starttls;
	}
	public void setStarttls(boolean starttls) {
		this.starttls = starttls;
	}
}
