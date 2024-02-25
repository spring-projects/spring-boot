/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email support.
 *
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * SMTP server host. For instance, 'smtp.example.com'.
	 */
	private String host;

	/**
	 * SMTP server port.
	 */
	private Integer port;

	/**
	 * Login user of the SMTP server.
	 */
	private String username;

	/**
	 * Login password of the SMTP server.
	 */
	private String password;

	/**
	 * Protocol used by the SMTP server.
	 */
	private String protocol = "smtp";

	/**
	 * Default MimeMessage encoding.
	 */
	private Charset defaultEncoding = DEFAULT_CHARSET;

	/**
	 * Additional JavaMail Session properties.
	 */
	private final Map<String, String> properties = new HashMap<>();

	/**
	 * Session JNDI name. When set, takes precedence over other Session settings.
	 */
	private String jndiName;

	/**
	 * Returns the host of the mail properties.
	 * @return the host of the mail properties
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host for the mail properties.
	 * @param host the host to be set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Returns the port number for the mail server.
	 * @return the port number for the mail server
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port number for the mail server.
	 * @param port the port number to be set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Returns the username associated with the MailProperties object.
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for the MailProperties.
	 * @param username the username to be set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password associated with the MailProperties object.
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the email account.
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the protocol used for sending and receiving emails.
	 * @return the protocol used for sending and receiving emails
	 */
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the protocol for the mail properties.
	 * @param protocol the protocol to be set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Returns the default encoding used by the MailProperties class.
	 * @return the default encoding
	 */
	public Charset getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Sets the default encoding for the MailProperties.
	 * @param defaultEncoding the default encoding to be set
	 */
	public void setDefaultEncoding(Charset defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Returns the properties of the MailProperties object.
	 * @return a Map containing the properties as key-value pairs
	 */
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
	 * Sets the JNDI name for the mail properties.
	 * @param jndiName the JNDI name to set
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * Returns the JNDI name associated with this MailProperties object.
	 * @return the JNDI name
	 */
	public String getJndiName() {
		return this.jndiName;
	}

}
