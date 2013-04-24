package org.springframework.bootstrap.sample.jetty;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class JettyBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(JettyBootstrapApplication.class, args);
	}

}
