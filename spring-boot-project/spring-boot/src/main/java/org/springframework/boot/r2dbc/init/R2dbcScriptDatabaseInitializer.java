/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.r2dbc.init;

import java.nio.charset.Charset;
import java.util.List;

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
	protected void runScripts(List<Resource> scripts, boolean continueOnError, String separator, Charset encoding) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(continueOnError);
		populator.setSeparator(separator);
		if (encoding != null) {
			populator.setSqlScriptEncoding(encoding.name());
		}
		for (Resource script : scripts) {
			populator.addScript(script);
		}
		populator.populate(this.connectionFactory).block();
	}

}
