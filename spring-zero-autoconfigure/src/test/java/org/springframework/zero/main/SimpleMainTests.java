/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.main;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.zero.main.Spring;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class SimpleMainTests {

	private ApplicationContext context;

	@After
	public void close() {
		if (this.context instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.context).close();
		}
	}

	@Test
	public void emptyApplicationContext() throws Exception {
		Spring.main(new String[0]);
		this.context = Spring.getApplicationContext();
		assertNotNull(this.context);
	}

	@Test
	public void basePackageScan() throws Exception {
		Spring.main(new String[] { ClassUtils.getPackageName(Spring.class) });
		this.context = Spring.getApplicationContext();
		assertNotNull(this.context);
	}

	@Test
	public void xmlContext() throws Exception {
		Spring.main(new String[] { Spring.class.getName(),
				"org/springframework/zero/sample-beans.xml" });
		this.context = Spring.getApplicationContext();
		assertNotNull(this.context);
	}

}
