/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.mail;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Provider;
import javax.mail.Provider.Type;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

import org.junit.Before;
import org.junit.Test;

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
public class MailHealthIndicatorTests {

	private JavaMailSenderImpl mailSender;

	private MailHealthIndicator indicator;

	@Before
	public void setup() {
		Session session = Session.getDefaultInstance(new Properties());
		session.addProvider(new Provider(Type.TRANSPORT, "success",
				SuccessTransport.class.getName(), "Test", "1.0.0"));
		this.mailSender = mock(JavaMailSenderImpl.class);
		given(this.mailSender.getHost()).willReturn("smtp.acme.org");
		given(this.mailSender.getPort()).willReturn(25);
		given(this.mailSender.getSession()).willReturn(session);
		this.indicator = new MailHealthIndicator(this.mailSender);
	}

	@Test
	public void smtpIsUp() {
		given(this.mailSender.getProtocol()).willReturn("success");
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("location")).isEqualTo("smtp.acme.org:25");
	}

	@Test
	public void smtpIsDown() throws MessagingException {
		willThrow(new MessagingException("A test exception")).given(this.mailSender)
				.testConnection();
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("location")).isEqualTo("smtp.acme.org:25");
		Object errorMessage = health.getDetails().get("error");
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.toString().contains("A test exception")).isTrue();
	}

	public static class SuccessTransport extends Transport {

		public SuccessTransport(Session session, URLName urlName) {
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
