/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Spring Batch with MongoDB.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers(disabledWithoutDocker = true)
class SampleBatchApplicationTests {

	@Container
	static final MongoDBContainer mongoDb = TestImage.forContainer(MongoDBContainer.class).withReplicaSet();

	@Test
	void testDefaultSettings(CapturedOutput output) {
		int exitCode = SpringApplication.exit(SpringApplication.run(SampleBatchApplication.class,
				"--spring.mongodb.uri=" + mongoDb.getReplicaSetUrl(),
				"--spring.batch.data.mongodb.schema.initialize=true"));
		assertThat(exitCode).isZero();
		assertThat(output).contains("completed with the following parameters");
	}

}
