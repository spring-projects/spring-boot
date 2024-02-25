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

package smoketest.actuator.customsecurity;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * SampleActuatorCustomSecurityApplication class.
 */
@SpringBootApplication
public class SampleActuatorCustomSecurityApplication {

	/**
     * The main method is the entry point of the application.
     * It starts the Spring Boot application by running the SpringApplication.run() method.
     * 
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
		SpringApplication.run(SampleActuatorCustomSecurityApplication.class, args);
	}

	/**
     * Creates a new instance of the TestServletEndpoint class.
     * 
     * @return the TestServletEndpoint instance
     */
    @Bean
	TestServletEndpoint servletEndpoint() {
		return new TestServletEndpoint();
	}

	/**
     * TestServletEndpoint class.
     */
    @ServletEndpoint(id = "se1")
	static class TestServletEndpoint implements Supplier<EndpointServlet> {

		/**
         * Returns an instance of EndpointServlet for the TestServletEndpoint class.
         * 
         * @return an instance of EndpointServlet for the TestServletEndpoint class
         */
        @Override
		public EndpointServlet get() {
			return new EndpointServlet(ExampleServlet.class);
		}

	}

	/**
     * ExampleServlet class.
     */
    static class ExampleServlet extends HttpServlet {

		/**
         * This method is called when a GET request is made to the servlet.
         * 
         * @param req the HttpServletRequest object representing the request made to the servlet
         * @param resp the HttpServletResponse object representing the response to be sent back to the client
         * @throws ServletException if there is a servlet-related problem
         * @throws IOException if there is an I/O problem
         */
        @Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		}

	}

}
