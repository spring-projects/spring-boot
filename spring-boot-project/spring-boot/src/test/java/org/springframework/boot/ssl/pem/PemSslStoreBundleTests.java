/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.ssl.pem;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link PemSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class PemSslStoreBundleTests {

	private static final String CERTIFICATE = """
			-----BEGIN CERTIFICATE-----
			MIIDqzCCApOgAwIBAgIIFMqbpqvipw0wDQYJKoZIhvcNAQELBQAwbDELMAkGA1UE
			BhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCVBhbG8gQWx0bzEP
			MA0GA1UEChMGVk13YXJlMQ8wDQYDVQQLEwZTcHJpbmcxEjAQBgNVBAMTCWxvY2Fs
			aG9zdDAgFw0yMzA1MDUxMTI2NThaGA8yMTIzMDQxMTExMjY1OFowbDELMAkGA1UE
			BhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCVBhbG8gQWx0bzEP
			MA0GA1UEChMGVk13YXJlMQ8wDQYDVQQLEwZTcHJpbmcxEjAQBgNVBAMTCWxvY2Fs
			aG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPwHWxoE3xjRmNdD
			+m+e/aFlr5wEGQUdWSDD613OB1w7kqO/audEp3c6HxDB3GPcEL0amJwXgY6CQMYu
			sythuZX/EZSc2HdilTBu/5T+mbdWe5JkKThpiA0RYeucQfKuB7zv4ypioa4wiR4D
			nPsZXjg95OF8pCzYEssv8wT49v+M3ohWUgfF0FPlMFCSo0YVTuzB1mhDlWKq/jhQ
			11WpTmk/dQX+l6ts6bYIcJt4uItG+a68a4FutuSjZdTAE0f5SOYRBpGH96mjLwEP
			fW8ZjzvKb9g4R2kiuoPxvCDs1Y/8V2yvKqLyn5Tx9x/DjFmOi0DRK/TgELvNceCb
			UDJmhXMCAwEAAaNPME0wHQYDVR0OBBYEFMBIGU1nwix5RS3O5hGLLoMdR1+NMCwG
			A1UdEQQlMCOCCWxvY2FsaG9zdIcQAAAAAAAAAAAAAAAAAAAAAYcEfwAAATANBgkq
			hkiG9w0BAQsFAAOCAQEAhepfJgTFvqSccsT97XdAZfvB0noQx5NSynRV8NWmeOld
			hHP6Fzj6xCxHSYvlUfmX8fVP9EOAuChgcbbuTIVJBu60rnDT21oOOnp8FvNonCV6
			gJ89sCL7wZ77dw2RKIeUFjXXEV3QJhx2wCOVmLxnJspDoKFIEVjfLyiPXKxqe/6b
			dG8zzWDZ6z+M2JNCtVoOGpljpHqMPCmbDktncv6H3dDTZ83bmLj1nbpOU587gAJ8
			fl1PiUDyPRIl2cnOJd+wCHKsyym/FL7yzk0OSEZ81I92LpGd/0b2Ld3m/bpe+C4Z
			ILzLXTnC6AhrLcDc9QN/EO+BiCL52n7EplNLtSn1LQ==
			-----END CERTIFICATE-----
			""".strip();

	private static final String PRIVATE_KEY = """
			-----BEGIN PRIVATE KEY-----
			MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD8B1saBN8Y0ZjX
			Q/pvnv2hZa+cBBkFHVkgw+tdzgdcO5Kjv2rnRKd3Oh8Qwdxj3BC9GpicF4GOgkDG
			LrMrYbmV/xGUnNh3YpUwbv+U/pm3VnuSZCk4aYgNEWHrnEHyrge87+MqYqGuMIke
			A5z7GV44PeThfKQs2BLLL/ME+Pb/jN6IVlIHxdBT5TBQkqNGFU7swdZoQ5Viqv44
			UNdVqU5pP3UF/perbOm2CHCbeLiLRvmuvGuBbrbko2XUwBNH+UjmEQaRh/epoy8B
			D31vGY87ym/YOEdpIrqD8bwg7NWP/Fdsryqi8p+U8fcfw4xZjotA0Sv04BC7zXHg
			m1AyZoVzAgMBAAECggEAfEqiZqANaF+BqXQIb4Dw42ZTJzWsIyYYnPySOGZRoe5t
			QJ03uwtULYv34xtANe1DQgd6SMyc46ugBzzjtprQ3ET5Jhn99U6kdcjf+dpf85dO
			hOEppP0CkDNI39nleinSfh6uIOqYgt/D143/nqQhn8oCdSOzkbwT9KnWh1bC9T7I
			vFjGfElvt1/xl88qYgrWgYLgXaencNGgiv/4/M0FNhiHEGsVC7SCu6kapC/WIQpE
			5IdV+HR+tiLoGZhXlhqorY7QC4xKC4wwafVSiFxqDOQAuK+SMD4TCEv0Aop+c+SE
			YBigVTmgVeJkjK7IkTEhKkAEFmRF5/5w+bZD9FhTNQKBgQD+4fNG1ChSU8RdizZT
			5dPlDyAxpETSCEXFFVGtPPh2j93HDWn7XugNyjn5FylTH507QlabC+5wZqltdIjK
			GRB5MIinQ9/nR2fuwGc9s+0BiSEwNOUB1MWm7wWL/JUIiKq6sTi6sJIfsYg79zco
			qxl5WE94aoINx9Utq1cdWhwJTQKBgQD9IjPksd4Jprz8zMrGLzR8k1gqHyhv24qY
			EJ7jiHKKAP6xllTUYwh1IBSL6w2j5lfZPpIkb4Jlk2KUoX6fN81pWkBC/fTBUSIB
			EHM9bL51+yKEYUbGIy/gANuRbHXsWg3sjUsFTNPN4hGTFk3w2xChCyl/f5us8Lo8
			Z633SNdpvwKBgQCGyDU9XzNzVZihXtx7wS0sE7OSjKtX5cf/UCbA1V0OVUWR3SYO
			J0HPCQFfF0BjFHSwwYPKuaR9C8zMdLNhK5/qdh/NU7czNi9fsZ7moh7SkRFbzJzN
			OxbKD9t/CzJEMQEXeF/nWTfsSpUgILqqZtAxuuFLbAcaAnJYlCKdAumQgQKBgQCK
			mqjJh68pn7gJwGUjoYNe1xtGbSsqHI9F9ovZ0MPO1v6e5M7sQJHH+Fnnxzv/y8e8
			d6tz8e73iX1IHymDKv35uuZHCGF1XOR+qrA/KQUc+vcKf21OXsP/JtkTRs1HLoRD
			S5aRf2DWcfvniyYARSNU2xTM8GWgi2ueWbMDHUp+ZwKBgA/swC+K+Jg5DEWm6Sau
			e6y+eC6S+SoXEKkI3wf7m9aKoZo0y+jh8Gas6gratlc181pSM8O3vZG0n19b493I
			apCFomMLE56zEzvyzfpsNhFhk5MBMCn0LPyzX6MiynRlGyWIj0c99fbHI3pOMufP
			WgmVLTZ8uDcSW1MbdUCwFSk5
			-----END PRIVATE KEY-----
			""".strip();

	private static final char[] EMPTY_KEY_PASSWORD = new char[] {};

	@Test
	void createWithDetailsWhenNullStores() {
		PemSslStoreDetails keyStoreDetails = null;
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void createWithDetailsWhenStoresHaveNoValues() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(null);
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(null);
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void createWithDetailsWhenHasKeyStoreDetailsCertAndKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void createWithDetailsWhenHasKeyStoreDetailsCertAndEncryptedKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:ssl/pkcs8/key-rsa-encrypted.pem")
			.withPrivateKeyPassword("test");
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void createWithDetailsWhenHasKeyStoreDetailsAndTrustStoreDetailsWithoutKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCert("ssl"));
	}

	@Test
	void createWithDetailsWhenHasKeyStoreDetailsAndTrustStoreDetails() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("ssl"));
	}

	@Test
	void createWithDetailsWhenHasEmbeddedKeyStoreDetailsAndTrustStoreDetails() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(CERTIFICATE).withPrivateKey(PRIVATE_KEY);
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(CERTIFICATE)
			.withPrivateKey(PRIVATE_KEY);
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("ssl"));
	}

	@Test
	void createWithDetailsWhenHasStoreType() {
		PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:test-cert.pem",
				"classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:test-cert.pem",
				"classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
	}

	@Test
	void createWithDetailsWhenHasKeyStoreDetailsAndTrustStoreDetailsAndKeyPassword() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem")
			.withAlias("ksa")
			.withPassword("kss");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem")
			.withAlias("tsa")
			.withPassword("tss");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ksa", "kss".toCharArray()));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("tsa", "tss".toCharArray()));
	}

	@Test
	void createWithPemSslStoreCreatesInstance() {
		List<X509Certificate> certificates = PemContent.of(CERTIFICATE).getCertificates();
		PrivateKey privateKey = PemContent.of(PRIVATE_KEY).getPrivateKey();
		PemSslStore pemSslStore = PemSslStore.of(certificates, privateKey);
		PemSslStoreBundle bundle = new PemSslStoreBundle(pemSslStore, pemSslStore);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("ssl"));
	}

	@Test
	void storeCreationIsLazy() {
		PemSslStore pemSslStore = mock(PemSslStore.class);
		PemSslStoreBundle bundle = new PemSslStoreBundle(pemSslStore, pemSslStore);
		given(pemSslStore.certificates()).willReturn(PemContent.of(CERTIFICATE).getCertificates());
		then(pemSslStore).shouldHaveNoInteractions();
		bundle.getKeyStore();
		then(pemSslStore).should().certificates();
		bundle.getTrustStore();
		then(pemSslStore).should(times(2)).certificates();
	}

	private Consumer<KeyStore> storeContainingCert(String keyAlias) {
		return storeContainingCert(KeyStore.getDefaultType(), keyAlias);
	}

	private Consumer<KeyStore> storeContainingCert(String keyStoreType, String keyAlias) {
		return ThrowingConsumer.of((keyStore) -> {
			assertThat(keyStore).isNotNull();
			assertThat(keyStore.getType()).isEqualTo(keyStoreType);
			assertThat(keyStore.containsAlias(keyAlias)).isTrue();
			assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
			assertThat(keyStore.getKey(keyAlias, EMPTY_KEY_PASSWORD)).isNull();
		});
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias) {
		return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias) {
		return storeContainingCertAndKey(keyStoreType, keyAlias, EMPTY_KEY_PASSWORD);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, char[] keyPassword) {
		return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, char[] keyPassword) {
		return ThrowingConsumer.of((keyStore) -> {
			assertThat(keyStore).isNotNull();
			assertThat(keyStore.getType()).isEqualTo(keyStoreType);
			assertThat(keyStore.containsAlias(keyAlias)).isTrue();
			assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
			assertThat(keyStore.getKey(keyAlias, keyPassword)).isNotNull();
		});
	}

}
