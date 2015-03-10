/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link HealthIndicator} for configured smtp server(s).
 *
 * @author Johannes Stelzer
 * @since 1.3.0
 */
public class MailHealthIndicator extends AbstractHealthIndicator {

	private final JavaMailSenderImpl mailSender;

	public MailHealthIndicator(JavaMailSenderImpl mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		String location = String.format("%s:%d", this.mailSender.getHost(), this.mailSender.getPort());
		builder.withDetail("location", location);

		Transport transport = null;
		try {
			transport = connectTransport();
			builder.up();
		}
		finally {
			if (transport != null) {
				transport.close();
			}
		}
	}

	// Copy-paste from JavaMailSenderImpl - see SPR-12799
	private Transport connectTransport() throws MessagingException {
		String username = this.mailSender.getUsername();
		String password = this.mailSender.getPassword();
		if ("".equals(username)) {
			username = null;
			if ("".equals(password)) {
				password = null;
			}
		}

		Transport transport = getTransport(this.mailSender.getSession());
		transport.connect(this.mailSender.getHost(), this.mailSender.getPort(), username,
				password);
		return transport;
	}

	private Transport getTransport(Session session) throws NoSuchProviderException {
		String protocol = this.mailSender.getProtocol();
		if (protocol == null) {
			protocol = session.getProperty("mail.transport.protocol");
			if (protocol == null) {
				protocol = JavaMailSenderImpl.DEFAULT_PROTOCOL;
			}
		}
		return session.getTransport(protocol);
	}

}
