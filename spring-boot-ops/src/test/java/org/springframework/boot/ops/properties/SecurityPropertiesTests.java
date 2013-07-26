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

package org.springframework.boot.ops.properties;

import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.ops.properties.SecurityProperties;
import org.springframework.boot.strap.bind.RelaxedDataBinder;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link SecurityProperties}.
 * 
 * @author Dave Syer
 */
public class SecurityPropertiesTests {

	@Test
	public void testBindingIgnoredSingleValued() {
		SecurityProperties security = new SecurityProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"security.ignored", "/css/**")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(1, security.getIgnored().length);
	}

	@Test
	public void testBindingIgnoredMultiValued() {
		SecurityProperties security = new SecurityProperties();
		RelaxedDataBinder binder = new RelaxedDataBinder(security, "security");
		binder.setConversionService(new DefaultConversionService());
		binder.bind(new MutablePropertyValues(Collections.singletonMap(
				"security.ignored", "/css/**,/images/**")));
		assertFalse(binder.getBindingResult().hasErrors());
		assertEquals(2, security.getIgnored().length);
	}

}
