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
package org.springframework.bootstrap.actuate.properties;

import org.junit.Test;
import org.springframework.bootstrap.actuate.properties.EndpointsProperties;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Externalized configuration for endpoints (e.g. paths)
 * 
 * @author Dave Syer
 * 
 */
public class EndpointsPropertiesTests {

	private EndpointsProperties properties = new EndpointsProperties();

	@Test
	public void testDefaultPathValid() throws Exception {
		assertEquals("/error", this.properties.getError().getPath());
		Errors errors = validate(this.properties);
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testQueryPathValid() throws Exception {
		Errors errors = validate(new EndpointsProperties.Endpoint("/foo?bar"));
		assertFalse(errors.hasErrors());
	}

	@Test
	public void testEmptyPathInvalid() throws Exception {
		Errors errors = validate(new EndpointsProperties.Endpoint(""));
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testDoubleSlashInvalid() throws Exception {
		Errors errors = validate(new EndpointsProperties.Endpoint("//foo"));
		assertTrue(errors.hasErrors());
	}

	@Test
	public void testEmptyPathInProperties() throws Exception {
		this.properties.getError().setPath("");
		Errors errors = validate(this.properties);
		assertTrue(errors.hasErrors());
	}

	/**
	 * @return
	 */
	private Errors validate(Object target) {
		BindException errors = new BindException(target, "properties");
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		validator.validate(target, errors);
		return errors;
	}

}
