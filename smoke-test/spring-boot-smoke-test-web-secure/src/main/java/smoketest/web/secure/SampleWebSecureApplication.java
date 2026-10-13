/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SampleWebSecureApplication implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/login").setViewName("login");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureApplication.class).run(args);
	}

}
