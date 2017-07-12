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
 * Tests for {@link CloudFoundryEndpointHandlerMapping}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryEndpointHandlerMappingTests {

	// TODO CloudFoundry support

	// @Test
	// public void getHandlerExecutionChainWhenEndpointHasPathShouldMapAgainstName()
	// throws Exception {
	// TestMvcEndpoint testMvcEndpoint = new TestMvcEndpoint(new TestEndpoint("a"));
	// testMvcEndpoint.setPath("something-else");
	// CloudFoundryEndpointHandlerMapping handlerMapping = new
	// CloudFoundryEndpointHandlerMapping(
	// Collections.singleton(testMvcEndpoint), null, null);
	// assertThat(handlerMapping.getPath(testMvcEndpoint)).isEqualTo("/a");
	// }
	//
	// @Test
	// public void doesNotRegisterHalJsonMvcEndpoint() throws Exception {
	// CloudFoundryEndpointHandlerMapping handlerMapping = new
	// CloudFoundryEndpointHandlerMapping(
	// Collections.singleton(new TestHalJsonMvcEndpoint()), null, null);
	// assertThat(handlerMapping.getEndpoints()).hasSize(0);
	// }
	//
	// @Test
	// public void registersCloudFoundryDiscoveryEndpoint() throws Exception {
	// StaticApplicationContext context = new StaticApplicationContext();
	// CloudFoundryEndpointHandlerMapping handlerMapping = new
	// CloudFoundryEndpointHandlerMapping(
	// Collections.<NamedMvcEndpoint>emptySet(), null, null);
	// handlerMapping.setPrefix("/test");
	// handlerMapping.setApplicationContext(context);
	// handlerMapping.afterPropertiesSet();
	// HandlerExecutionChain handler = handlerMapping
	// .getHandler(new MockHttpServletRequest("GET", "/test"));
	// HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
	// assertThat(handlerMethod.getBean())
	// .isInstanceOf(CloudFoundryDiscoveryMvcEndpoint.class);
	// }
	//
	// @Test
	// public void registersCloudFoundryHealthEndpoint() throws Exception {
	// StaticApplicationContext context = new StaticApplicationContext();
	// HealthEndpoint delegate = new HealthEndpoint(new OrderedHealthAggregator(),
	// Collections.<String, HealthIndicator>emptyMap());
	// CloudFoundryEndpointHandlerMapping handlerMapping = new
	// CloudFoundryEndpointHandlerMapping(
	// Collections.singleton(new TestHealthMvcEndpoint(delegate)), null, null);
	// handlerMapping.setPrefix("/test");
	// handlerMapping.setApplicationContext(context);
	// handlerMapping.afterPropertiesSet();
	// HandlerExecutionChain handler = handlerMapping
	// .getHandler(new MockHttpServletRequest("GET", "/test/health"));
	// HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
	// Object handlerMethodBean = handlerMethod.getBean();
	// assertThat(handlerMethodBean).isInstanceOf(CloudFoundryHealthMvcEndpoint.class);
	// }
	//
	// private static class TestEndpoint extends AbstractEndpoint<Object> {
	//
	// TestEndpoint(String id) {
	// super(id);
	// }
	//
	// @Override
	// public Object invoke() {
	// return null;
	// }
	//
	// }
	//
	// private static class TestMvcEndpoint extends EndpointMvcAdapter {
	//
	// TestMvcEndpoint(TestEndpoint delegate) {
	// super(delegate);
	// }
	//
	// }
	//
	// private static class TestHalJsonMvcEndpoint extends HalJsonMvcEndpoint {
	//
	// TestHalJsonMvcEndpoint() {
	// super(new ManagementServletContext() {
	//
	// @Override
	// public String getContextPath() {
	// return "";
	// }
	//
	// });
	// }
	//
	// }
	//
	// private static class TestHealthMvcEndpoint extends HealthWebEndpointExtension {
	//
	// TestHealthMvcEndpoint(HealthEndpoint delegate) {
	// super(delegate);
	// }
	//
	// }

}
