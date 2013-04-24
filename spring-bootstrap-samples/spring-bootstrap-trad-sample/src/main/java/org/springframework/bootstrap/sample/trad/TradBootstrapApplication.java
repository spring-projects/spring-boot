package org.springframework.bootstrap.sample.trad;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class TradBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TradBootstrapApplication.class, args);
	}

}
