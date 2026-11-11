/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link LdapSslSocketFactory}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class LdapSslSocketFactoryTests {

	@AfterEach
	void clearSslBundle() {
		LdapSslSocketFactory.setSslBundle(null);
	}

	@Test
	void shouldFailWhenNoSslBundleHasBeenSet() {
		assertThatIllegalStateException().isThrownBy(LdapSslSocketFactory::getDefault)
			.withMessage("No SSL bundle has been set");
	}

	@Test
	void shouldUseSocketFactoryFromSslBundle() {
		SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
		LdapSslSocketFactory.setSslBundle(sslBundle(socketFactory));
		assertThat(LdapSslSocketFactory.getDefault()).isSameAs(socketFactory);
	}

	@Test
	void shouldCreateSocketFactoryOnce() {
		SslBundle sslBundle = sslBundle(mock(SSLSocketFactory.class));
		LdapSslSocketFactory.setSslBundle(sslBundle);
		assertThat(LdapSslSocketFactory.getDefault()).isSameAs(LdapSslSocketFactory.getDefault());
		then(sslBundle).should(times(1)).createSslContext();
	}

	@Test
	void shouldKeepSocketFactoryWhenSettingTheSameBundleAgain() {
		SslBundle sslBundle = sslBundle(mock(SSLSocketFactory.class));
		LdapSslSocketFactory.setSslBundle(sslBundle);
		LdapSslSocketFactory.getDefault();
		LdapSslSocketFactory.setSslBundle(sslBundle);
		LdapSslSocketFactory.getDefault();
		then(sslBundle).should(times(1)).createSslContext();
	}

	@Test
	void shouldUseSocketFactoryOfNewBundleWhenBundleIsReplaced() {
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		LdapSslSocketFactory.getDefault();
		SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
		LdapSslSocketFactory.setSslBundle(sslBundle(socketFactory));
		assertThat(LdapSslSocketFactory.getDefault()).isSameAs(socketFactory);
	}

	@Test
	void shouldFailWhenSslBundleHasBeenCleared() {
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		LdapSslSocketFactory.setSslBundle(null);
		assertThatIllegalStateException().isThrownBy(LdapSslSocketFactory::getDefault)
			.withMessage("No SSL bundle has been set");
	}

	@Test
	void shouldWarnWhenReplacingBundleWithADifferentOne(CapturedOutput output) {
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		LdapSslSocketFactory.setSslBundle(sslBundle(mock(SSLSocketFactory.class)));
		assertThat(output).contains("A different SSL bundle has already been set for LDAP");
	}

	@Test
	void shouldNotWarnWhenSettingTheSameBundleAgain(CapturedOutput output) {
		SslBundle sslBundle = sslBundle(mock(SSLSocketFactory.class));
		LdapSslSocketFactory.setSslBundle(sslBundle);
		LdapSslSocketFactory.setSslBundle(sslBundle);
		assertThat(output).doesNotContain("A different SSL bundle has already been set for LDAP");
	}

	private SslBundle sslBundle(SSLSocketFactory socketFactory) {
		SSLContext sslContext = mock(SSLContext.class);
		given(sslContext.getSocketFactory()).willReturn(socketFactory);
		SslBundle sslBundle = mock(SslBundle.class);
		given(sslBundle.createSslContext()).willReturn(sslContext);
		return sslBundle;
	}

}
