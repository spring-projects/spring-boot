package org.springframework.bootstrap.sample.ui;

import java.util.Date;
import java.util.Map;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.actuate.autoconfigure.ConditionalOnManagementContext;
import org.springframework.bootstrap.actuate.autoconfigure.ManagementAutoConfiguration;
import org.springframework.bootstrap.actuate.autoconfigure.SecurityAutoConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnExpression;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class,
		ManagementAutoConfiguration.class })
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

	@Configuration
	@ConditionalOnExpression("${server.port:8080} != ${management.port:${server.port:8080}}")
	@Import(ManagementAutoConfiguration.class)
	protected static class ManagementConfiguration {

	}

	@Configuration
	@ConditionalOnExpression("${server.port:8080} != ${management.port:${server.port:8080}}")
	@ConditionalOnManagementContext
	@Import(SecurityAutoConfiguration.class)
	protected static class ManagementSecurityConfiguration {

	}

}
