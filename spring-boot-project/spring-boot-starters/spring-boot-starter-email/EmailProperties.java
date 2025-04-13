package org.springframework.boot.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.mail")
public class EmailProperties {

	private String host;
	private int port;
	private String username;
	private String password;

	// Getters and setters
}
