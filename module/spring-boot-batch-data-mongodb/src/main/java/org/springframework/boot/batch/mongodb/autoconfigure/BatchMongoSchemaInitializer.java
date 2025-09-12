/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.batch.mongodb.autoconfigure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.StreamUtils;

/**
 * Initializes the Spring Batch job repository schema (collections and indexes) in MongoDB
 * by executing commands from a newline-delimited JSON script.
 *
 * @author Stephane Nicoll
 */
class BatchMongoSchemaInitializer {

	private static final Log logger = LogFactory.getLog(BatchMongoSchemaInitializer.class);

	private final MongoOperations mongoOperations;

	BatchMongoSchemaInitializer(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	void initialize(Resource schema) throws IOException {
		StreamUtils.copyToString(schema.getInputStream(), StandardCharsets.UTF_8)
			.lines()
			.filter((line) -> !line.isBlank())
			.forEach((command) -> {
				try {
					executeCommand(command);
				}
				catch (Exception ex) {
					throw new BatchMongoSchemaInitializationException(
							"Failed to initialize Batch repository schema using '%s', command failed: %s"
								.formatted(schema, command),
							ex);
				}
			});
	}

	private void executeCommand(String command) {
		if (logger.isTraceEnabled()) {
			logger.trace("Executing command: " + command);
		}
		this.mongoOperations.executeCommand(command);
	}

}
