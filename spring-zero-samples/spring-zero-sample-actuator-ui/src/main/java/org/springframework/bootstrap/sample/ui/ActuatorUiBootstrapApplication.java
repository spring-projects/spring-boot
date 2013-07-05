package org.springframework.bootstrap.sample.ui;

import java.util.Date;
import java.util.Map;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.actuate.properties.SecurityProperties;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@EnableAutoConfiguration
@ComponentScan
@Controller
public class ActuatorUiBootstrapApplication {

	@RequestMapping("/")
	public String home(Map<String, Object> model) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return "home";
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ActuatorUiBootstrapApplication.class, args);
	}

	@Bean
	public SecurityProperties securityProperties() {
		SecurityProperties security = new SecurityProperties();
		security.getBasic().setPath(""); // empty
		return security;
	}

}
