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

package org.springframework.boot.autoconfigure.hateoas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.hateoas.LinkDiscoverer;
import org.springframework.hateoas.LinkDiscoverers;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.hal.HalLinkDiscoverer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests for {@link HypermediaAutoConfiguration}.
 *
 * @author Roy Clarkson
 */
public class HypermediaAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void linkDiscoverersCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(HypermediaAutoConfiguration.class);
		this.context.refresh();
		LinkDiscoverers discoverers = this.context.getBean(LinkDiscoverers.class);
		assertNotNull(discoverers);
		LinkDiscoverer discoverer = discoverers.getLinkDiscovererFor(MediaTypes.HAL_JSON);
		assertTrue(HalLinkDiscoverer.class.isInstance(discoverer));
	}

}
