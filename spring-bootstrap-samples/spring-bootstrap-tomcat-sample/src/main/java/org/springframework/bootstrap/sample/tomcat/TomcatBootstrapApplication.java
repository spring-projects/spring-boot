package org.springframework.bootstrap.sample.tomcat;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class TomcatBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TomcatBootstrapApplication.class, args);
	}

}
