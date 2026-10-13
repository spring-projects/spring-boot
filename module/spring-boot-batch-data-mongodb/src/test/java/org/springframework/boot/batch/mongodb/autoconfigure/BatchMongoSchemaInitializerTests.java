/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.mongodb.autoconfigure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoOperations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchMongoSchemaInitializer}.
 *
 * @author Stephane Nicoll
 */
class BatchMongoSchemaInitializerTests {

	@Test
	void initializeExecutesEachNonBlankLine() throws IOException {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		BatchMongoSchemaInitializer initializer = new BatchMongoSchemaInitializer(mongoOperations);
		Resource schema = resource("{\"create\": \"batch_job_instance\"}\n{\"create\": \"batch_job_execution\"}");

		initializer.initialize(schema);

		then(mongoOperations).should().executeCommand("{\"create\": \"batch_job_instance\"}");
		then(mongoOperations).should().executeCommand("{\"create\": \"batch_job_execution\"}");
	}

	@Test
	void initializeSkipsBlankLines() throws IOException {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		BatchMongoSchemaInitializer initializer = new BatchMongoSchemaInitializer(mongoOperations);
		Resource schema = resource("{\"create\": \"col1\"}\n\n  \n{\"create\": \"col2\"}");

		initializer.initialize(schema);

		then(mongoOperations).should().executeCommand("{\"create\": \"col1\"}");
		then(mongoOperations).should().executeCommand("{\"create\": \"col2\"}");
	}

	@Test
	void initializeThrowsWhenCommandFails() {
		MongoOperations mongoOperations = mock(MongoOperations.class);
		given(mongoOperations.executeCommand(anyString())).willThrow(new RuntimeException("Command failed"));
		BatchMongoSchemaInitializer initializer = new BatchMongoSchemaInitializer(mongoOperations);
		Resource schema = resource("{\"create\": \"batch_job_instance\"}");

		assertThatExceptionOfType(BatchMongoSchemaInitializationException.class)
			.isThrownBy(() -> initializer.initialize(schema))
			.withMessageContaining("Failed to initialize Batch repository schema")
			.withMessageContaining("{\"create\": \"batch_job_instance\"}")
			.withCauseInstanceOf(RuntimeException.class);
	}

	private static Resource resource(String content) {
		return new InputStreamResource(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
	}

}
