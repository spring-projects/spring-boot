package org.springframework.boot.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
@EnableConfigurationProperties(EmailProperties.class)
public class EmailAutoConfiguration {

	@Bean
	public EmailService emailService(JavaMailSender javaMailSender) {
		return new EmailService(javaMailSender);
	}
}
