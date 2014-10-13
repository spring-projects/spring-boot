package org.springframework.boot.autoconfigure.hdiv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hdiv.config.HDIVConfig;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link HdivAutoConfiguration}.
 */
public class HdivAutoConfigurationTests {

	private static final MockEmbeddedServletContainerFactory containerFactory = new MockEmbeddedServletContainerFactory();

	private AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfig() throws Exception {
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				HdivAutoConfiguration.class);
		this.context.refresh();
		HDIVConfig config = this.context.getBean(HDIVConfig.class);
		assertTrue(config.isStartPage("/", "get"));
		assertTrue(config.isStartPage("/example.js", "get"));

		assertTrue(config.isStartParameter("_csrf"));

		assertTrue(config.existValidations());
		assertTrue(config.areEditableParameterValuesValid("/", "paramName", new String[] { "paramValue" }, "text"));
		assertFalse(config.areEditableParameterValuesValid("/", "paramName", new String[] { "<script>XSS</script>" },
				"text"));
	}

	@Configuration
	public static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

}
