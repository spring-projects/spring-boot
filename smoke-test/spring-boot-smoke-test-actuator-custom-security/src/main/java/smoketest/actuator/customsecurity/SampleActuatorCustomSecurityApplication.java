/*
 * Copyright 2012-present the original author or authors.
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

@SpringBootApplication
@SuppressWarnings("removal")
public class SampleActuatorCustomSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleActuatorCustomSecurityApplication.class, args);
	}

	@Bean
	TestServletEndpoint servletEndpoint() {
		return new TestServletEndpoint();
	}

	@ServletEndpoint(id = "se1")
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
