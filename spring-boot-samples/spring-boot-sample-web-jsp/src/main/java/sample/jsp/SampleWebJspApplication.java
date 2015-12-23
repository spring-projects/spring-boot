/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class SampleWebJspApplication extends SpringBootServletInitializer {
	private static final Log log = LogFactory.getLog(SampleWebJspApplication.class);

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleWebJspApplication.class);
	}

	public static void main(String[] args) throws Exception {
		try {
			SpringApplication.run(SampleWebJspApplication.class, args);
		} catch (Exception e) {
			Throwable rootCause = getRootCause(e);
			if (rootCause instanceof ClassNotFoundException && rootCause.getMessage().contains("javax.servlet.ServletContext")) {
				log.error("The Servlet class could not be found. If you are running this as Spring Boot run configuration " +
						"from within IntelliJ 15.01 or lower, you might be experiencing the following problem: " +
						"https://youtrack.jetbrains.com/issue/IDEA-107048 . If you want this application to work in " +
						"IntelliJ, you can consider\n" +
						"1) Creating a Tomcat configuration in IntelliJ,\n" +
						"2) Making a profile in the pom.xml which removes the <scope>provided</scope> from the dependencies when running from within IntelliJ, or\n" +
						"3) Creating a mvn spring-boot:run configuration.");
			}
			throw e;
		}
	}

	private static Throwable getRootCause(Throwable e) {
		if (e.getCause() == null) return e;
		else return getRootCause(e.getCause());
	}
}
