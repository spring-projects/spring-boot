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

package org.springframework.boot.autoconfigure.mail;

import java.net.SocketTimeoutException;
import java.security.cert.CertPathBuilderException;
import java.time.Duration;
import java.util.Arrays;

import javax.net.ssl.SSLException;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.MailpitContainer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Integration tests for {@link MailSenderAutoConfiguration}.
 *
 * @author Rui Figueira
 */
@Testcontainers(disabledWithoutDocker = true)
class MailSenderAutoConfigurationIntegrationTests {

	private SimpleMailMessage createMessage(String subject) {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom("from@example.com");
		msg.setTo("to@example.com");
		msg.setSubject(subject);
		msg.setText("Subject: " + subject);
		return msg;
	}

	private String getSubject(Message message) {
		try {
			return message.getSubject();
		}
		catch (MessagingException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void assertMessagesContainSubject(Session session, String subject) {
		try (Store store = session.getStore("pop3")) {
			String host = session.getProperty("mail.pop3.host");
			int port = Integer.parseInt(session.getProperty("mail.pop3.port"));
			store.connect(host, port, "user", "pass");
			try (Folder folder = store.getFolder("inbox")) {
				folder.open(Folder.READ_ONLY);
				Awaitility.await()
					.atMost(Duration.ofSeconds(5))
					.ignoreExceptions()
					.untilAsserted(() -> assertThat(Arrays.stream(folder.getMessages()).map(this::getSubject))
						.contains(subject));
			}
		}
		catch (MessagingException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Nested
	class ImplicitTlsTests {

		final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class, SslAutoConfiguration.class));

		@Container
		static final MailpitContainer mailpit = new MailpitContainer()
			// force ssl connection
			.withSmtpRequireTls(true)
			.withSmtpTlsCert(MountableFile
				.forClasspathResource("/org/springframework/boot/autoconfigure/mail/ssl/test-server.crt"))
			.withSmtpTlsKey(MountableFile
				.forClasspathResource("/org/springframework/boot/autoconfigure/mail/ssl/test-server.key"))
			.withPop3Auth("user:pass");

		@Test
		void sendEmailWithSslEnabledAndCert() {
			this.contextRunner.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
					"spring.mail.port:" + mailpit.getSmtpPort(), "spring.mail.ssl.enabled:true",
					"spring.mail.ssl.bundle:test-bundle",
					"spring.ssl.bundle.pem.test-bundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-ca.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.private-key=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.key",
					// pop3
					"spring.mail.properties.mail.pop3.host:" + mailpit.getHost(),
					"spring.mail.properties.mail.pop3.port:" + mailpit.getPop3Port())
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					mailSender.send(createMessage("Hello World!"));

					assertMessagesContainSubject(mailSender.getSession(), "Hello World!");
				});
		}

		@Test
		void sendEmailWithSslEnabledWithoutCert() {
			this.contextRunner
				.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
						"spring.mail.port:" + mailpit.getSmtpPort(), "spring.mail.ssl.enabled:true")
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					assertThatException().isThrownBy(() -> mailSender.send(createMessage("Should fail")))
						.withRootCauseInstanceOf(CertPathBuilderException.class);
				});
		}

		@Test
		void sendEmailWithoutSslWithCert() {
			this.contextRunner.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
					"spring.mail.port:" + mailpit.getSmtpPort(), "spring.mail.properties.mail.smtp.timeout:1000",
					"spring.mail.ssl.bundle:test-bundle",
					"spring.ssl.bundle.pem.test-bundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-ca.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.private-key=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.key")
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					assertThatException().isThrownBy(() -> mailSender.send(createMessage("Should fail")))
						.withRootCauseInstanceOf(SocketTimeoutException.class);
				});
		}

	}

	@Nested
	class StarttlsTests {

		final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class, SslAutoConfiguration.class));

		@Container
		static final MailpitContainer mailpit = new MailpitContainer().withSmtpRequireStarttls(true)
			.withSmtpTlsCert(MountableFile
				.forClasspathResource("/org/springframework/boot/autoconfigure/mail/ssl/test-server.crt"))
			.withSmtpTlsKey(MountableFile
				.forClasspathResource("/org/springframework/boot/autoconfigure/mail/ssl/test-server.key"))
			.withPop3Auth("user:pass");

		@Test
		void sendEmailWithStarttlsAndCertAndSslDisabled() {
			this.contextRunner.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
					"spring.mail.port:" + mailpit.getSmtpPort(),
					"spring.mail.properties.mail.smtp.starttls.enable:true",
					"spring.mail.properties.mail.smtp.starttls.required:true", "spring.mail.ssl.bundle:test-bundle",
					"spring.ssl.bundle.pem.test-bundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-ca.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.private-key=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.key",
					// pop3
					"spring.mail.properties.mail.pop3.host:" + mailpit.getHost(),
					"spring.mail.properties.mail.pop3.port:" + mailpit.getPop3Port())
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					mailSender.send(createMessage("Sent with STARTTLS"));

					assertMessagesContainSubject(mailSender.getSession(), "Sent with STARTTLS");
				});
		}

		@Test
		void sendEmailWithStarttlsAndCertAndSslEnabled() {
			this.contextRunner.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
					"spring.mail.port:" + mailpit.getSmtpPort(), "spring.mail.ssl.enabled:true",
					"spring.mail.properties.mail.smtp.starttls.enable:true",
					"spring.mail.properties.mail.smtp.starttls.required:true", "spring.mail.ssl.bundle:test-bundle",
					"spring.ssl.bundle.pem.test-bundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-ca.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.certificate=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.crt",
					"spring.ssl.bundle.pem.test-bundle.keystore.private-key=classpath:org/springframework/boot/autoconfigure/mail/ssl/test-client.key",
					// pop3
					"spring.mail.properties.mail.pop3.host:" + mailpit.getHost(),
					"spring.mail.properties.mail.pop3.port:" + mailpit.getPop3Port())
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					assertThatException().isThrownBy(() -> mailSender.send(createMessage("Should fail")))
						.withRootCauseInstanceOf(SSLException.class);
				});
		}

		@Test
		void sendEmailWithStarttlsWithoutCert() {
			this.contextRunner
				.withPropertyValues("spring.mail.host:" + mailpit.getHost(),
						"spring.mail.port:" + mailpit.getSmtpPort(),
						"spring.mail.properties.mail.smtp.starttls.enable:true",
						"spring.mail.properties.mail.smtp.starttls.required:true")
				.run((context) -> {
					JavaMailSenderImpl mailSender = context.getBean(JavaMailSenderImpl.class);

					assertThatException().isThrownBy(() -> mailSender.send(createMessage("Should fail")))
						.withRootCauseInstanceOf(CertPathBuilderException.class);
				});
		}

	}

}
