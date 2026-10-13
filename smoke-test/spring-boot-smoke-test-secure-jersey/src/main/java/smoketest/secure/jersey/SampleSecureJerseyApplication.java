/*
 * Copyright 2012-present the original author or authors.
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
