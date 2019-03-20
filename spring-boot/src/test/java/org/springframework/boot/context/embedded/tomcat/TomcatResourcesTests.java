/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Context;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatResources}.
 *
 * @author Phillip Webb
 */
public class TomcatResourcesTests {

	@Test
	public void nonHierarchicalUri() throws Exception {
		// gh-13265
		Context context = mock(Context.class);
		TomcatResources resources = new TomcatResources.Tomcat8Resources(context);
		List<URL> resourceJarUrls = new ArrayList<URL>();
		resourceJarUrls.add(new URL("jar:file:/opt/app/app.war!/WEB-INF/classes!/"));
		resources.addResourceJars(resourceJarUrls);
	}

}
