/*
 * Copyright 2012-present the original author or authors.
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
			assertThat(converted).isNotNull();
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
