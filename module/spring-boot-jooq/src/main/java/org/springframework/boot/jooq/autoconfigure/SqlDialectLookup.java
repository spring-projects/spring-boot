/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;
import org.jspecify.annotations.Nullable;

/**
 * Utility to lookup well known {@link SQLDialect SQLDialects} from a {@link DataSource}.
 *
 * @author Michael Simons
 * @author Lukas Eder
 * @author Ramil Saetov
 */
final class SqlDialectLookup {

	private static final Log logger = LogFactory.getLog(SqlDialectLookup.class);

	private SqlDialectLookup() {
	}

	/**
	 * Return the most suitable {@link SQLDialect} for the given {@link DataSource}.
	 * @param dataSource the source {@link DataSource}
	 * @return the most suitable {@link SQLDialect}
	 */
	static SQLDialect getDialect(@Nullable DataSource dataSource) {
		try (Connection connection = (dataSource != null) ? dataSource.getConnection() : null) {
			return JDBCUtils.dialect(connection);
		}
		catch (SQLException ex) {
			logger.warn("Unable to determine dialect from datasource", ex);
		}
		return SQLDialect.DEFAULT;
	}

}
