/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.testcontainers.springbeans;

import org.junit.jupiter.api.Test;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MyTestConfiguration.class)
class MyIntegrationTests {

	@Autowired
	private MongoDBContainer mongo;

	@Test
	void myTest() {
		/**/ System.out.println(this.mongo);
	}

}
