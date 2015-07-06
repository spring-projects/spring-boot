package org.springframework.boot.actuate.endpoint;

import org.flywaydb.core.Flyway;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FlywayEndpoint}.
 *
 * @author Eddú Meléndez
 */
public class FlywayEndpointTests extends AbstractEndpointTests<FlywayEndpoint> {

	public FlywayEndpointTests() {
		super(Config.class, FlywayEndpoint.class, "flyway", false, "endpoints.flyway");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke().size(), is(1));
	}

	@Configuration
	@Import({EmbeddedDataSourceConfiguration.class, FlywayAutoConfiguration.class})
	public static class Config {

		@Autowired
		private Flyway flyway;

		@Bean
		public FlywayEndpoint endpoint() {
			return new FlywayEndpoint(this.flyway);
		}

	}

}
