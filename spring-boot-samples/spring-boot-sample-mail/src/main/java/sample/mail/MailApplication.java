package sample.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MailApplication implements CommandLineRunner {

	@Autowired
	private MailService mailService;

	@Value("${sample.message.to}")
	private String to;

	@Value("${sample.message.text}")
	private String text;

	@Override
	public void run(String... args) {
		Message message = new Message(to, text);
		mailService.processMessage(message);
	}

	public static void main(String[] args) {
		SpringApplication.run(MailApplication.class, args);
	}

}
