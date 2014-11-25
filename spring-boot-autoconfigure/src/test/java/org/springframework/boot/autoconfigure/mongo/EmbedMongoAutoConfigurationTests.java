/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.CommandResult;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

public class EmbedMongoAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shouldReturnVersionOfEmbeddedMongoServer() {
		this.context = new AnnotationConfigApplicationContext();
		int mongoPort = findAvailableTcpPort();
		addEnvironment(context, "spring.data.mongodb.host=localhost", "spring.data.mongodb.port=" + mongoPort);
		context.register(MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, EmbedMongoAutoConfiguration.class);
		context.refresh();
		MongoTemplate mongo = context.getBean(MongoTemplate.class);
		CommandResult buildInfo = mongo.executeCommand("{ buildInfo: 1 }");
		assertEquals("2.6.1", buildInfo.getString("version"));
	}

}
