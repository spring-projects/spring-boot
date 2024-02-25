/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.web.secure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SampleWebSecureApplication class.
 */
@SpringBootApplication
public class SampleWebSecureApplication implements WebMvcConfigurer {

	/**
     * Adds view controllers to the registry.
     * 
     * @param registry the ViewControllerRegistry to add the view controllers to
     */
    @Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/login").setViewName("login");
	}

	/**
     * The main method is the entry point of the application.
     * It initializes and runs the Spring Boot application using the SpringApplicationBuilder.
     * 
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
		new SpringApplicationBuilder(SampleWebSecureApplication.class).run(args);
	}

}
