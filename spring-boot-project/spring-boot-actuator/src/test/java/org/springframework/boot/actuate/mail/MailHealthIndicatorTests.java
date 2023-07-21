/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.mail;

import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Provider;
import jakarta.mail.Provider.Type;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MailHealthIndicator}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 */
class MailHealthIndicatorTests {

	private JavaMailSenderImpl mailSender;

	private MailHealthIndicator indicator;

	@BeforeEach
	void setup() {
		Session session = Session.getDefaultInstance(new Properties());
		session.addProvider(new Provider(Type.TRANSPORT, "success", SuccessTransport.class.getName(), "Test", "1.0.0"));
		this.mailSender = mock(JavaMailSenderImpl.class);
		given(this.mailSender.getHost()).willReturn("smtp.acme.org");
		given(this.mailSender.getSession()).willReturn(session);
		this.indicator = new MailHealthIndicator(this.mailSender);
	}

	@Test
	void smtpOnDefaultPortIsUp() {
		given(this.mailSender.getPort()).willReturn(-1);
		given(this.mailSender.getProtocol()).willReturn("success");
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("location", "smtp.acme.org");
	}

	@Test
	void smtpOnDefaultPortIsDown() throws MessagingException {
		given(this.mailSender.getPort()).willReturn(-1);
		willThrow(new MessagingException("A test exception")).given(this.mailSender).testConnection();
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("location", "smtp.acme.org");
		Object errorMessage = health.getDetails().get("error");
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.toString().contains("A test exception")).isTrue();
	}

	@Test
	void smtpOnCustomPortIsUp() {
		given(this.mailSender.getPort()).willReturn(1234);
		given(this.mailSender.getProtocol()).willReturn("success");
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("location", "smtp.acme.org:1234");
	}

	@Test
	void smtpOnCustomPortIsDown() throws MessagingException {
		given(this.mailSender.getPort()).willReturn(1234);
		willThrow(new MessagingException("A test exception")).given(this.mailSender).testConnection();
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("location", "smtp.acme.org:1234");
		Object errorMessage = health.getDetails().get("error");
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.toString()).contains("A test exception");
	}

	static class SuccessTransport extends Transport {

		SuccessTransport(Session session, URLName urlName) {
			super(session, urlName);
		}

		@Override
		public void connect(String host, int port, String user, String password) {
		}

		@Override
		public void sendMessage(Message msg, Address[] addresses) {
		}

	}

}
