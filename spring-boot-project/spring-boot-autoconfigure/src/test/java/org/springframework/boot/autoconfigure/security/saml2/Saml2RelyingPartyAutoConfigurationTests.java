/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.saml2;

import java.util.List;

import javax.servlet.Filter;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Saml2RelyingPartyAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class Saml2RelyingPartyAutoConfigurationTests {

	private static final String PREFIX = "spring.security.saml2.relyingparty.registration";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(Saml2RelyingPartyAutoConfiguration.class, SecurityAutoConfiguration.class));

	private MockWebServer server;

	@AfterEach
	void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	void autoConfigurationShouldBeConditionalOnRelyingPartyRegistrationRepositoryClass() {
		this.contextRunner.withPropertyValues(getPropertyValues()).withClassLoader(new FilteredClassLoader(
				"org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository"))
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void autoConfigurationShouldBeConditionalOnServletWebApplication() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(Saml2RelyingPartyAutoConfiguration.class))
				.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void relyingPartyRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void relyingPartyRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner.withPropertyValues(getPropertyValues()).run((context) -> {
			RelyingPartyRegistrationRepository repository = context.getBean(RelyingPartyRegistrationRepository.class);
			RelyingPartyRegistration registration = repository.findByRegistrationId("foo");

			assertThat(registration.getAssertingPartyDetails().getSingleSignOnServiceLocation())
					.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php");
			assertThat(registration.getAssertingPartyDetails().getEntityId())
					.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php");
			assertThat(registration.getAssertionConsumerServiceLocation())
					.isEqualTo("{baseUrl}" + Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);
			assertThat(registration.getAssertingPartyDetails().getSingleSignOnServiceBinding())
					.isEqualTo(Saml2MessageBinding.POST);
			assertThat(registration.getAssertingPartyDetails().getWantAuthnRequestsSigned()).isEqualTo(false);
			assertThat(registration.getSigningX509Credentials()).isNotNull();
			assertThat(registration.getAssertingPartyDetails().getVerificationX509Credentials()).isNotNull();
			assertThat(registration.getEntityId()).isEqualTo("{baseUrl}/saml2/foo-entity-id");
		});
	}

	@Test
	void autoConfigurationWhenSignRequestsTrueAndNoSigningCredentialsShouldThrowException() {
		this.contextRunner.withPropertyValues(getPropertyValuesWithoutSigningCredentials(true)).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining(
					"Signing credentials must not be empty when authentication requests require signing.");
		});
	}

	@Test
	void autoConfigurationWhenSignRequestsFalseAndNoSigningCredentialsShouldNotThrowException() {
		this.contextRunner.withPropertyValues(getPropertyValuesWithoutSigningCredentials(false))
				.run((context) -> assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void autoconfigurationShouldQueryIdentityProviderMetadataWhenMetadataUrlIsPresent() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String metadataUrl = this.server.url("").toString();
		setupMockResponse();
		this.contextRunner.withPropertyValues(PREFIX + ".foo.identityprovider.metadata-url=" + metadataUrl)
				.run((context) -> {
					assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
					assertThat(this.server.getRequestCount()).isEqualTo(1);
				});
	}

	@Test
	void relyingPartyRegistrationRepositoryShouldBeConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(getPropertyValues())
				.withUserConfiguration(RegistrationRepositoryConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
					assertThat(context).hasBean("testRegistrationRepository");
				});
	}

	@Test
	void samlLoginShouldBeConfigured() {
		this.contextRunner.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(hasFilter(context, Saml2WebSsoAuthenticationFilter.class)).isTrue());
	}

	@Test
	void samlLoginShouldBackOffWhenAWebSecurityConfigurerAdapterIsDefined() {
		this.contextRunner.withUserConfiguration(WebSecurityConfigurerAdapterConfiguration.class)
				.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(hasFilter(context, Saml2WebSsoAuthenticationFilter.class)).isFalse());
	}

	@Test
	void samlLoginShouldBackOffWhenASecurityFilterChainBeanIsPresent() {
		this.contextRunner.withUserConfiguration(TestSecurityFilterChainConfig.class)
				.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(hasFilter(context, Saml2WebSsoAuthenticationFilter.class)).isFalse());
	}

	@Test
	void samlLoginShouldShouldBeConditionalOnSecurityWebFilterClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
				.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
	}

	private String[] getPropertyValuesWithoutSigningCredentials(boolean signRequests) {
		return new String[] { PREFIX
				+ ".foo.identityprovider.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.identityprovider.singlesignon.binding=post",
				PREFIX + ".foo.identityprovider.singlesignon.sign-request=" + signRequests,
				PREFIX + ".foo.identityprovider.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.identityprovider.verification.credentials[0].certificate-location=classpath:saml/certificate-location" };
	}

	private String[] getPropertyValues() {
		return new String[] {
				PREFIX + ".foo.signing.credentials[0].private-key-location=classpath:saml/private-key-location",
				PREFIX + ".foo.signing.credentials[0].certificate-location=classpath:saml/certificate-location",
				PREFIX + ".foo.identityprovider.singlesignon.url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				PREFIX + ".foo.identityprovider.singlesignon.binding=post",
				PREFIX + ".foo.identityprovider.singlesignon.sign-request=false",
				PREFIX + ".foo.identityprovider.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				PREFIX + ".foo.identityprovider.verification.credentials[0].certificate-location=classpath:saml/certificate-location",
				PREFIX + ".foo.relying-party-entity-id={baseUrl}/saml2/foo-entity-id" };
	}

	private boolean hasFilter(AssertableWebApplicationContext context, Class<? extends Filter> filter) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(0).getFilters();
		return filters.stream().anyMatch(filter::isInstance);
	}

	private void setupMockResponse() {
		String metadataResponse = "<md:EntityDescriptor entityID=\"https://idp.example.com/idp/shibboleth\"\n"
				+ "                     xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n"
				+ "                     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
				+ "                     xmlns:shibmd=\"urn:mace:shibboleth:metadata:1.0\"\n"
				+ "                     xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n"
				+ "                     xmlns:mdui=\"urn:oasis:names:tc:SAML:metadata:ui\">\n" + "    \n"
				+ "   <md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
				+ "      <md:KeyDescriptor>\n" + "         <ds:KeyInfo>\n" + "            <ds:X509Data>\n"
				+ "               <ds:X509Certificate>\n"
				+ "                  MIIDZjCCAk6gAwIBAgIVAL9O+PA7SXtlwZZY8MVSE9On1cVWMA0GCSqGSIb3DQEB\n"
				+ "                  BQUAMCkxJzAlBgNVBAMTHmlkZW0tcHVwYWdlbnQuZG16LWludC51bmltby5pdDAe\n"
				+ "                  Fw0xMzA3MjQwMDQ0MTRaFw0zMzA3MjQwMDQ0MTRaMCkxJzAlBgNVBAMTHmlkZW0t\n"
				+ "                  cHVwYWdlbnQuZG16LWludC51bmltby5pdDCCASIwDQYJKoZIhvcNAMIIDQADggEP\n"
				+ "                  ADCCAQoCggEBAIAcp/VyzZGXUF99kwj4NvL/Rwv4YvBgLWzpCuoxqHZ/hmBwJtqS\n"
				+ "                  v0y9METBPFbgsF3hCISnxbcmNVxf/D0MoeKtw1YPbsUmow/bFe+r72hZ+IVAcejN\n"
				+ "                  iDJ7t5oTjsRN1t1SqvVVk6Ryk5AZhpFW+W9pE9N6c7kJ16Rp2/mbtax9OCzxpece\n"
				+ "                  byi1eiLfIBmkcRawL/vCc2v6VLI18i6HsNVO3l2yGosKCbuSoGDx2fCdAOk/rgdz\n"
				+ "                  cWOvFsIZSKuD+FVbSS/J9GVs7yotsS4PRl4iX9UMnfDnOMfO7bcBgbXtDl4SCU1v\n"
				+ "                  dJrRw7IL/pLz34Rv9a8nYitrzrxtLOp3nYUCAwEAAaOBhDCBgTBgBgMIIDEEWTBX\n"
				+ "                  gh5pZGVtLXB1cGFnZW50LmRtei1pbnQudW5pbW8uaXSGNWh0dHBzOi8vaWRlbS1w\n"
				+ "                  dXBhZ2VudC5kbXotaW50LnVuaW1vLml0L2lkcC9zaGliYm9sZXRoMB0GA1UdDgQW\n"
				+ "                  BBT8PANzz+adGnTRe8ldcyxAwe4VnzANBgkqhkiG9w0BAQUFAAOCAQEAOEnO8Clu\n"
				+ "                  9z/Lf/8XOOsTdxJbV29DIF3G8KoQsB3dBsLwPZVEAQIP6ceS32Xaxrl6FMTDDNkL\n"
				+ "                  qUvvInUisw0+I5zZwYHybJQCletUWTnz58SC4C9G7FpuXHFZnOGtRcgGD1NOX4UU\n"
				+ "                  duus/4nVcGSLhDjszZ70Xtj0gw2Sn46oQPHTJ81QZ3Y9ih+Aj1c9OtUSBwtWZFkU\n"
				+ "                  yooAKoR8li68Yb21zN2N65AqV+ndL98M8xUYMKLONuAXStDeoVCipH6PJ09Z5U2p\n"
				+ "                  V5p4IQRV6QBsNw9CISJFuHzkVYTH5ZxzN80Ru46vh4y2M0Nu8GQ9I085KoZkrf5e\n"
				+ "                  Cq53OZt9ISjHEw==\n" + "               </ds:X509Certificate>\n"
				+ "            </ds:X509Data>\n" + "         </ds:KeyInfo>\n" + "      </md:KeyDescriptor>\n" + "   \n"
				+ "      <md:SingleSignOnService\n"
				+ "         Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n"
				+ "         Location=\"https://idp.example.com/sso\"/>\n" + "   </md:IDPSSODescriptor>\n" + "    \n"
				+ "   <md:ContactPerson contactType=\"technical\">\n"
				+ "      <md:EmailAddress>mailto:technical.contact@example.com</md:EmailAddress>\n"
				+ "   </md:ContactPerson>\n" + "    \n" + "</md:EntityDescriptor>";
		MockResponse mockResponse = new MockResponse().setBody(metadataResponse);
		this.server.enqueue(mockResponse);
	}

	@Configuration(proxyBeanMethods = false)
	static class RegistrationRepositoryConfiguration {

		@Bean
		RelyingPartyRegistrationRepository testRegistrationRepository() {
			return mock(RelyingPartyRegistrationRepository.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebSecurityConfigurerAdapterConfiguration {

		@Bean
		WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
			return new WebSecurityConfigurerAdapter() {

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
			return http.antMatcher("/**").authorizeRequests((authorize) -> authorize.anyRequest().authenticated())
					.build();
		}

	}

}
