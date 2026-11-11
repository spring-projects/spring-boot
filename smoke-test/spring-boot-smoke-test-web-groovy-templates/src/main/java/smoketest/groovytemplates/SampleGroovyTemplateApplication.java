/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.groovytemplates;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

@SpringBootApplication
public class SampleGroovyTemplateApplication {

	@Bean
	public MessageRepository messageRepository() {
		return new InMemoryMessageRepository();
	}

	@Bean
	public Converter<String, @Nullable Message> messageConverter() {
		return (id) -> messageRepository().findMessage(Long.valueOf(id));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleGroovyTemplateApplication.class, args);
	}

}
