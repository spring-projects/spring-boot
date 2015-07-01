package org.springframework.boot.actuate.hypermedia.test;

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootHypermediaApplication {

	@Bean
	public TemplateEngine groovyTemplateEngine() {
		return new GStringTemplateEngine();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringBootHypermediaApplication.class, args);
	}
}
