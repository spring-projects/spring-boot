package org.springframework.boot.autoconfigure.mustache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.AutoApplicationTests.Application;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest({ "server.port:0",
		"spring.mustache.prefix:classpath:/mustache-templates/" })
@WebAppConfiguration
public class AutoApplicationTests {

	@Autowired
	private EmbeddedWebApplicationContext context;
	private int port;

	@Before
	public void init() {
		this.port = this.context.getEmbeddedServletContainer().getPort();
	}

	@Test
	public void testHomePage() throws Exception {
		String body = new TestRestTemplate().getForObject(
				"http://localhost:" + this.port, String.class);
		assertTrue(body.contains("Hello World"));
	}

	@Test
	public void testPartialPage() throws Exception {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port
				+ "/partial", String.class);
		assertTrue(body.contains("Hello World"));
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ MustacheAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected static @interface MinimalWebConfiguration {
	}

	@Configuration
	@MinimalWebConfiguration
	@Controller
	public static class Application {

		@RequestMapping("/")
		public String home(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "home";
		}

		@RequestMapping("/partial")
		public String layout(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "partial";
		}

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

	}

}
