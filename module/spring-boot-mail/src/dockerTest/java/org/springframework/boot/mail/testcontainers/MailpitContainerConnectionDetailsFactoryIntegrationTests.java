/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.testcontainers;

import java.time.Duration;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.MailpitContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Mailpit service connections.
 *
 * @author Van Vo
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class MailpitContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final MailpitContainer mailpit = TestImage.forContainer(MailpitContainer.class).withPop3Auth("user:pass");

	@Autowired
	private JavaMailSender mailSender;

	@Test
	void sendsMailToContainer() throws Exception {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("from@example.com");
		message.setTo("to@example.com");
		message.setSubject("Service connection");
		message.setText("Mail sent using a service connection");
		this.mailSender.send(message);
		Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (Store store = Session.getInstance(new Properties()).getStore("pop3")) {
				store.connect(mailpit.getHost(), mailpit.getPop3Port(), "user", "pass");
				try (Folder folder = store.getFolder("inbox")) {
					folder.open(Folder.READ_ONLY);
					assertThat(folder.getMessageCount()).isEqualTo(1);
					assertThat(folder.getMessage(1).getSubject()).isEqualTo(message.getSubject());
				}
			}
		});
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(MailSenderAutoConfiguration.class)
	static class TestConfiguration {

	}

}
