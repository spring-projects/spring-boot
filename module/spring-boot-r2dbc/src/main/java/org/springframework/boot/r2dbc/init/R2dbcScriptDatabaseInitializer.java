/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.init;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.boot.sql.init.AbstractScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * An {@link InitializingBean} that initializes a database represented by an R2DBC
 * {@link ConnectionFactory}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class R2dbcScriptDatabaseInitializer extends AbstractScriptDatabaseInitializer {

	private final ConnectionFactory connectionFactory;

	/**
	 * Creates a new {@code R2dbcScriptDatabaseInitializer} that will initialize the
	 * database recognized by the given {@code connectionFactory} using the given
	 * {@code settings}.
	 * @param connectionFactory connectionFactory for the database
	 * @param settings initialization settings
	 */
	public R2dbcScriptDatabaseInitializer(ConnectionFactory connectionFactory,
			DatabaseInitializationSettings settings) {
		super(settings);
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected boolean isEmbeddedDatabase() {
		return EmbeddedDatabaseConnection.isEmbedded(this.connectionFactory);
	}

	@Override
	protected void runScripts(Scripts scripts) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(scripts.isContinueOnError());
		populator.setSeparator(scripts.getSeparator());
		if (scripts.getEncoding() != null) {
			populator.setSqlScriptEncoding(scripts.getEncoding().name());
		}
		for (Resource script : scripts) {
			populator.addScript(script);
		}
		populator.populate(this.connectionFactory).block();
	}

}
