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

package org.springframework.boot.actuate.cloudfoundry;

/**
 * Tests for {@link CloudFoundryActuatorAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryActuatorAutoConfigurationTests {

	// TODO CloudFoundry support

	// private AnnotationConfigWebApplicationContext context;
	//
	// @Before
	// public void setup() {
	// this.context = new AnnotationConfigWebApplicationContext();
	// this.context.setServletContext(new MockServletContext());
	// this.context.register(SecurityAutoConfiguration.class,
	// WebMvcAutoConfiguration.class,
	// ManagementWebSecurityAutoConfiguration.class,
	// JacksonAutoConfiguration.class,
	// HttpMessageConvertersAutoConfiguration.class,
	// EndpointAutoConfiguration.class, EndpointServletWebAutoConfiguration.class,
	// PropertyPlaceholderAutoConfiguration.class,
	// RestTemplateAutoConfiguration.class,
	// EndpointWebMvcManagementContextConfiguration.class,
	// CloudFoundryActuatorAutoConfiguration.class);
	// }
	//
	// @After
	// public void close() {
	// if (this.context != null) {
	// this.context.close();
	// }
	// }
	//
	// @Test
	// public void cloudFoundryPlatformActive() throws Exception {
	// CloudFoundryEndpointHandlerMapping handlerMapping = getHandlerMapping();
	// assertThat(handlerMapping.getPrefix()).isEqualTo("/cloudfoundryapplication");
	// CorsConfiguration corsConfiguration = (CorsConfiguration) ReflectionTestUtils
	// .getField(handlerMapping, "corsConfiguration");
	// assertThat(corsConfiguration.getAllowedOrigins()).contains("*");
	// assertThat(corsConfiguration.getAllowedMethods()).containsAll(
	// Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name()));
	// assertThat(corsConfiguration.getAllowedHeaders()).containsAll(
	// Arrays.asList("Authorization", "X-Cf-App-Instance", "Content-Type"));
	// }
	//
	// @Test
	// public void cloudFoundryPlatformActiveSetsApplicationId() throws Exception {
	// CloudFoundryEndpointHandlerMapping handlerMapping = getHandlerMapping();
	// Object interceptor = ReflectionTestUtils.getField(handlerMapping,
	// "securityInterceptor");
	// String applicationId = (String) ReflectionTestUtils.getField(interceptor,
	// "applicationId");
	// assertThat(applicationId).isEqualTo("my-app-id");
	// }
	//
	// @Test
	// public void cloudFoundryPlatformActiveSetsCloudControllerUrl() throws Exception {
	// CloudFoundryEndpointHandlerMapping handlerMapping = getHandlerMapping();
	// Object interceptor = ReflectionTestUtils.getField(handlerMapping,
	// "securityInterceptor");
	// Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
	// "cloudFoundrySecurityService");
	// String cloudControllerUrl = (String) ReflectionTestUtils
	// .getField(interceptorSecurityService, "cloudControllerUrl");
	// assertThat(cloudControllerUrl).isEqualTo("http://my-cloud-controller.com");
	// }
	//
	// @Test
	// public void skipSslValidation() throws Exception {
	// TestPropertyValues.of("management.cloudfoundry.skipSslValidation:true")
	// .applyTo(this.context);
	// ConfigurationPropertySources.attach(this.context.getEnvironment());
	// this.context.refresh();
	// CloudFoundryEndpointHandlerMapping handlerMapping = getHandlerMapping();
	// Object interceptor = ReflectionTestUtils.getField(handlerMapping,
	// "securityInterceptor");
	// Object interceptorSecurityService = ReflectionTestUtils.getField(interceptor,
	// "cloudFoundrySecurityService");
	// RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils
	// .getField(interceptorSecurityService, "restTemplate");
	// assertThat(restTemplate.getRequestFactory())
	// .isInstanceOf(SkipSslVerificationHttpRequestFactory.class);
	// }
	//
	// @Test
	// public void cloudFoundryPlatformActiveAndCloudControllerUrlNotPresent()
	// throws Exception {
	// TestPropertyValues
	// .of("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
	// .applyTo(this.context);
	// this.context.refresh();
	// CloudFoundryEndpointHandlerMapping handlerMapping = this.context.getBean(
	// "cloudFoundryEndpointHandlerMapping",
	// CloudFoundryEndpointHandlerMapping.class);
	// Object securityInterceptor = ReflectionTestUtils.getField(handlerMapping,
	// "securityInterceptor");
	// Object interceptorSecurityService = ReflectionTestUtils
	// .getField(securityInterceptor, "cloudFoundrySecurityService");
	// assertThat(interceptorSecurityService).isNull();
	// }
	//
	// @Test
	// public void cloudFoundryPathsIgnoredBySpringSecurity() throws Exception {
	// TestPropertyValues
	// .of("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id")
	// .applyTo(this.context);
	// this.context.refresh();
	// IgnoredRequestCustomizer customizer = (IgnoredRequestCustomizer) this.context
	// .getBean("cloudFoundryIgnoredRequestCustomizer");
	// IgnoredRequestConfigurer configurer = mock(IgnoredRequestConfigurer.class);
	// customizer.customize(configurer);
	// ArgumentCaptor<RequestMatcher> requestMatcher = ArgumentCaptor
	// .forClass(RequestMatcher.class);
	// verify(configurer).requestMatchers(requestMatcher.capture());
	// RequestMatcher matcher = requestMatcher.getValue();
	// MockHttpServletRequest request = new MockHttpServletRequest();
	// request.setServletPath("/cloudfoundryapplication/my-path");
	// assertThat(matcher.matches(request)).isTrue();
	// request.setServletPath("/some-other-path");
	// assertThat(matcher.matches(request)).isFalse();
	// }
	//
	// @Test
	// public void cloudFoundryPlatformInactive() throws Exception {
	// this.context.refresh();
	// assertThat(this.context.containsBean("cloudFoundryEndpointHandlerMapping"))
	// .isFalse();
	// }
	//
	// @Test
	// public void cloudFoundryManagementEndpointsDisabled() throws Exception {
	// TestPropertyValues
	// .of("VCAP_APPLICATION=---", "management.cloudfoundry.enabled:false")
	// .applyTo(this.context);
	// this.context.refresh();
	// assertThat(this.context.containsBean("cloudFoundryEndpointHandlerMapping"))
	// .isFalse();
	// }
	//
	// private CloudFoundryEndpointHandlerMapping getHandlerMapping() {
	// TestPropertyValues
	// .of("VCAP_APPLICATION:---", "vcap.application.application_id:my-app-id",
	// "vcap.application.cf_api:http://my-cloud-controller.com")
	// .applyTo(this.context);
	// this.context.refresh();
	// return this.context.getBean("cloudFoundryEndpointHandlerMapping",
	// CloudFoundryEndpointHandlerMapping.class);
	// }

}
