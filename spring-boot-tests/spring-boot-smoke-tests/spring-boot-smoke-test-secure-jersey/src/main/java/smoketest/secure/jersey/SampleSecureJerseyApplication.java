/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.secure.jersey;

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

@SpringBootApplication
@SuppressWarnings("removal")
public class SampleSecureJerseyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleSecureJerseyApplication.class, args);
	}

	@Bean
	TestServletEndpoint servletEndpoint() {
		return new TestServletEndpoint();
	}

	@ServletEndpoint(id = "se1")
	@SuppressWarnings("removal")
	static class TestServletEndpoint implements Supplier<EndpointServlet> {

		@Override
		public EndpointServlet get() {
			return new EndpointServlet(ExampleServlet.class);
		}

	}

	static class ExampleServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		}

	}

}
