package org.springframework.boot.bind;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.SimplerPropertySourcesBindingTests.TestConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfig.class)
@IntegrationTest
public class SimplerPropertySourcesBindingTests {

	@Value("${foo:}")
	private String foo;

	@Autowired
	private Wrapper properties;

	@Test
	public void overridingOfPropertiesWorksAsExpected() {
		assertThat(this.foo, is(this.properties.getFoo()));
	}

	@PropertySources({ @PropertySource("classpath:/override.properties"),
			@PropertySource("classpath:/some.properties") })
	@Configuration
	@EnableConfigurationProperties(Wrapper.class)
	public static class TestConfig {

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@ConfigurationProperties
	public static class Wrapper {
		private String foo;

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

}