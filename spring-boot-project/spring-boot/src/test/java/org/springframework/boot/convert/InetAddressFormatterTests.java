/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.convert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumingThat;

/**
 * Tests for {@link InetAddressFormatter}.
 *
 * @author Phillip Webb
 */
class InetAddressFormatterTests {

	@ConversionServiceTest
	void convertFromInetAddressToStringShouldConvert(ConversionService conversionService) {
		assumingThat(isResolvable("example.com"), () -> {
			InetAddress address = InetAddress.getByName("example.com");
			String converted = conversionService.convert(address, String.class);
			assertThat(converted).isEqualTo(address.getHostAddress());
		});
	}

	@ConversionServiceTest
	void convertFromStringToInetAddressShouldConvert(ConversionService conversionService) {
		assumingThat(isResolvable("example.com"), () -> {
			InetAddress converted = conversionService.convert("example.com", InetAddress.class);
			assertThat(converted.toString()).startsWith("example.com");
		});
	}

	@ConversionServiceTest
	void convertFromStringToInetAddressWhenHostDoesNotExistShouldThrowException(ConversionService conversionService) {
		String missingDomain = "ireallydontexist.example.com";
		assumingThat(!isResolvable("ireallydontexist.example.com"),
				() -> assertThatExceptionOfType(ConversionFailedException.class)
						.isThrownBy(() -> conversionService.convert(missingDomain, InetAddress.class)));
	}

	private boolean isResolvable(String host) {
		try {
			InetAddress.getByName(host);
			return true;
		}
		catch (UnknownHostException ex) {
			return false;
		}
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new InetAddressFormatter());
	}

}
