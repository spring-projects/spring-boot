/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.bind.convert;

import java.io.InputStream;
import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BinderConversionService}.
 *
 * @author Phillip Webb
 */
public class BinderConversionServiceTests {

	private ConversionService delegate;

	private BinderConversionService service;

	@Before
	public void setup() {
		this.delegate = mock(ConversionService.class);
		this.service = new BinderConversionService(this.delegate);
	}

	@Test
	public void createConversionServiceShouldAcceptNullConversionService()
			throws Exception {
		BinderConversionService service = new BinderConversionService(null);
		assertThat(service.canConvert(String.class, TestEnum.class)).isTrue();
		assertThat(service.canConvert(TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(TestEnum.class))).isTrue();
		assertThat(service.convert("ONE", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(service.convert("ONE", TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(TestEnum.class))).isEqualTo(TestEnum.ONE);
	}

	@Test
	public void canConvertShouldDelegateToConversionService() throws Exception {
		Class<String> from = String.class;
		Class<InputStream> to = InputStream.class;
		given(this.delegate.canConvert(from, to)).willReturn(true);
		assertThat(this.service.canConvert(from, to)).isEqualTo(true);
		verify(this.delegate).canConvert(from, to);
	}

	@Test
	public void canConvertTypeDescriptorShouldDelegateToConversionService()
			throws Exception {
		TypeDescriptor from = TypeDescriptor.valueOf(String.class);
		TypeDescriptor to = TypeDescriptor.valueOf(InputStream.class);
		given(this.delegate.canConvert(from, to)).willReturn(true);
		assertThat(this.service.canConvert(from, to)).isEqualTo(true);
		verify(this.delegate).canConvert(from, to);
	}

	@Test
	public void convertShouldDelegateToConversionService() throws Exception {
		String from = "foo";
		InputStream to = mock(InputStream.class);
		given(this.delegate.convert(from, InputStream.class)).willReturn(to);
		assertThat(this.service.convert(from, InputStream.class)).isEqualTo(to);
		verify(this.delegate).convert(from, InputStream.class);
	}

	@Test
	public void convertTargetTypeShouldDelegateToConversionService() throws Exception {
		String from = "foo";
		InputStream to = mock(InputStream.class);
		TypeDescriptor fromType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor toType = TypeDescriptor.valueOf(InputStream.class);
		given(this.delegate.convert(from, fromType, toType)).willReturn(to);
		assertThat(this.service.convert(from, fromType, toType)).isEqualTo(to);
		verify(this.delegate).convert(from, fromType, toType);
	}

	@Test
	public void convertShouldSwallowDelegateConversionFailedException() throws Exception {
		given(this.delegate.convert("one", TestEnum.class))
				.willThrow(new ConversionFailedException(null, null, null, null));
		assertThat(this.service.convert("one", TestEnum.class)).isEqualTo(TestEnum.ONE);
		verify(this.delegate).convert("one", TestEnum.class);
	}

	@Test
	public void conversionServiceShouldSupportEnums() throws Exception {
		this.service = new BinderConversionService(null);
		assertThat(this.service.canConvert(String.class, TestEnum.class)).isTrue();
		assertThat(this.service.convert("one", TestEnum.class)).isEqualTo(TestEnum.ONE);
		assertThat(this.service.convert("t-w-o", TestEnum.class)).isEqualTo(TestEnum.TWO);
	}

	@Test
	public void conversionServiceShouldSupportStringToCharArray() throws Exception {
		this.service = new BinderConversionService(null);
		assertThat(this.service.canConvert(String.class, char[].class)).isTrue();
		assertThat(this.service.convert("test", char[].class)).containsExactly('t', 'e',
				's', 't');
	}

	@Test
	public void conversionServiceShouldSupportStringToInetAddress() throws Exception {
		this.service = new BinderConversionService(null);
		assertThat(this.service.canConvert(String.class, InetAddress.class)).isTrue();
	}

	@Test
	public void conversionServiceShouldSupportInetAddressToString() throws Exception {
		this.service = new BinderConversionService(null);
		assertThat(this.service.canConvert(InetAddress.class, String.class)).isTrue();
	}

	@Test
	public void conversionServiceShouldSupportStringToResource() throws Exception {
		this.service = new BinderConversionService(null);
		Resource resource = this.service.convert(
				"org/springframework/boot/context/properties/bind/convert/resource.txt",
				Resource.class);
		assertThat(resource).isNotNull();
	}

	@Test
	public void conversionServiceShouldSupportStringToClass() throws Exception {
		this.service = new BinderConversionService(null);
		Class<?> converted = this.service.convert(InputStream.class.getName(),
				Class.class);
		assertThat(converted).isEqualTo(InputStream.class);
	}

	enum TestEnum {

		ONE, TWO

	}

}
