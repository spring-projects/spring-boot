package org.springframework.boot.actuate.endpoint;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * {@link Endpoint} to expose flyway info.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.flyway", ignoreUnknownFields = true)
public class FlywayEndpoint extends AbstractEndpoint<List<MigrationInfo>> {

	private Flyway flyway;

	public FlywayEndpoint(Flyway flyway) {
		super("flyway", false);
		this.flyway = flyway;
	}

	@Override
	public List<MigrationInfo> invoke() {
		return Arrays.asList(this.flyway.info().all());
	}
}
