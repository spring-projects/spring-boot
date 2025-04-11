package org.springframework.boot.autoconfigure.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.*;
import org.springframework.mail.SimpleMailMessage;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JavaMailSender.class)
@EnableConfigurationProperties(EmailProperties.class)
public class EmailAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public JavaMailSender javaMailSender(EmailProperties properties) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(properties.getHost());
		mailSender.setPort(properties.getPort());
		mailSender.setUsername(properties.getUsername());
		mailSender.setPassword(properties.getPassword());

		var props = mailSender.getJavaMailProperties();
		props.put("mail.smtp.auth", String.valueOf(properties.isAuth()));
		props.put("mail.smtp.starttls.enable", String.valueOf(properties.isStarttls()));

		return mailSender;
	}

	@Bean
	@ConditionalOnMissingBean
	public EmailService emailService(JavaMailSender javaMailSender) {
		return new EmailService(javaMailSender);
	}
}
