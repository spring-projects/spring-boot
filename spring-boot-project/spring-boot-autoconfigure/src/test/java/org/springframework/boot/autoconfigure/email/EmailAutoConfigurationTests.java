package org.springframework.boot.autoconfigure.email;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EmailAutoConfiguration.class))
			.withPropertyValues(
					"spring.mail.host=smtp.example.com",
					"spring.mail.port=587",
					"spring.mail.username=test@example.com",
					"spring.mail.password=secret"
			);

	@Test
	void emailBeansShouldLoad() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(JavaMailSender.class);
			assertThat(context).hasSingleBean(EmailService.class);
		});
	}
}
