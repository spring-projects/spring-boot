package org.springframework.boot.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class EmailService {

	private final JavaMailSender mailSender;
	private final String fromAddress;

	public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromAddress) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	public void sendEmail(String to, String subject, String text) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(text, true);  // Assuming the content is HTML

		helper.setFrom(fromAddress);

		mailSender.send(message);
	}
}
