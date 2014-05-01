/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.freemarker;

import java.io.StringWriter;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 * 
 * @author Andy Wilkinson
 */
public class FreeMarkerNonWebappTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void renderTemplate() throws Exception {
		this.context.register(FreeMarkerAutoConfiguration.class);
		this.context.refresh();
		freemarker.template.Configuration freemarker = this.context
				.getBean(freemarker.template.Configuration.class);
		StringWriter writer = new StringWriter();
		freemarker.getTemplate("message.ftl").process(this, writer);
		assertTrue("Wrong content: " + writer, writer.toString().contains("Hello World"));
	}

	public String getGreeting() {
		return "Hello World";
	}
}
