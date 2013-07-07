package org.springframework.zero.sample.actuator.ui;

import java.util.Date;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.actuate.properties.SecurityProperties;
import org.springframework.zero.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@ComponentScan
@Controller
public class SampleActuatorUiApplication {

	@RequestMapping("/")
	public String home(Map<String, Object> model) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return "home";
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleActuatorUiApplication.class, args);
	}

	@Bean
	public SecurityProperties securityProperties() {
		SecurityProperties security = new SecurityProperties();
		security.getBasic().setPath(""); // empty
		return security;
	}

}
