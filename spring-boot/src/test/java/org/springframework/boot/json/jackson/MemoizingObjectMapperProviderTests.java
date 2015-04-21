/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.json.jackson;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dan Paquette
 */
@RunWith(MockitoJUnitRunner.class)
public class MemoizingObjectMapperProviderTests {

	@Mock
	private ObjectMapperProvider mockDelegate;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testOnlyCreatedOnce() {
		when(this.mockDelegate.getObjectMapper()).thenReturn(new ObjectMapper());
		MemoizingObjectMapperProvider factory = new MemoizingObjectMapperProvider(
				this.mockDelegate);
		ObjectMapper firstMapper = factory.getObjectMapper();
		assertNotNull(firstMapper);
		assertEquals(firstMapper, factory.getObjectMapper());
		verify(this.mockDelegate, times(1)).getObjectMapper();
	}

	@Test
	public void testNullDelegateThrowsIllegalArgumentException() {
		this.exception.expect(IllegalArgumentException.class);
		new MemoizingObjectMapperProvider(null);
	}
}
