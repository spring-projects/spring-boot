package org.springframework.boot.actuate.endpoint;

import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * {@link Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.liquibase", ignoreUnknownFields = true)
public class LiquibaseEndpoint extends AbstractEndpoint<List<Map<String, ?>>> {

	private SpringLiquibase liquibase;

	public LiquibaseEndpoint(SpringLiquibase liquibase) {
		super("liquibase", false);
		this.liquibase = liquibase;
	}

	@Override
	public List<Map<String, ?>> invoke() {
		StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
		try {
			DatabaseFactory databaseFactory = DatabaseFactory.getInstance();
			JdbcConnection connection =
					new JdbcConnection(this.liquibase.getDataSource().getConnection());
			Database database = databaseFactory.findCorrectDatabaseImplementation(connection);
			return service.queryDatabaseChangeLogTable(database);
		} catch (DatabaseException e) {
			throw new IllegalStateException(e.getMessage());
		} catch (SQLException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}
}
