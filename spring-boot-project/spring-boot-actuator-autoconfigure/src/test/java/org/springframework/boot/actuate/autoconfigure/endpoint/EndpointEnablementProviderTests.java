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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointEnablementProvider}.
 *
 * @author Stephane Nicoll
 */
public class EndpointEnablementProviderTests {

	@Test
	public void defaultEnablementDisabled() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED);
		validate(enablement, false, "endpoint 'foo' is disabled by default");
	}

	@Test
	public void defaultEnablementDisabledWithGeneralEnablement() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED, "endpoints.default.enabled=true");
		validate(enablement, false, "endpoint 'foo' is disabled by default");
	}

	@Test
	public void defaultEnablementDisabledWithGeneralTechEnablement() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED, EndpointExposure.WEB,
				"endpoints.default.web.enabled=true");
		validate(enablement, false, "endpoint 'foo' (web) is disabled by default");
	}

	@Test
	public void defaultEnablementDisabledWithOverride() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED, "endpoints.foo.enabled=true");
		validate(enablement, true, "found property endpoints.foo.enabled");
	}

	@Test
	public void defaultEnablementDisabledWithTechOverride() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED, EndpointExposure.WEB,
				"endpoints.foo.web.enabled=true");
		validate(enablement, true, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void defaultEnablementDisabledWithIrrelevantTechOverride() {
		EndpointEnablement enablement = getEndpointEnablement("foo",
				DefaultEnablement.DISABLED, EndpointExposure.WEB,
				"endpoints.foo.jmx.enabled=true");
		validate(enablement, false, "endpoint 'foo' (web) is disabled by default");
	}

	@Test
	public void defaultEnablementEnabled() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED);
		validate(enablement, true, "endpoint 'bar' is enabled by default");
	}

	@Test
	public void defaultEnablementEnabledWithGeneralDisablement() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED, "endpoints.default.enabled=false");
		validate(enablement, true, "endpoint 'bar' is enabled by default");
	}

	@Test
	public void defaultEnablementEnabledWithGeneralTechDisablement() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED, EndpointExposure.JMX,
				"endpoints.default.jmx.enabled=false");
		validate(enablement, true, "endpoint 'bar' (jmx) is enabled by default");
	}

	@Test
	public void defaultEnablementEnabledWithOverride() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED, "endpoints.bar.enabled=false");
		validate(enablement, false, "found property endpoints.bar.enabled");
	}

	@Test
	public void defaultEnablementEnabledWithTechOverride() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED, EndpointExposure.JMX,
				"endpoints.bar.jmx.enabled=false");
		validate(enablement, false, "found property endpoints.bar.jmx.enabled");
	}

	@Test
	public void defaultEnablementEnabledWithIrrelevantTechOverride() {
		EndpointEnablement enablement = getEndpointEnablement("bar",
				DefaultEnablement.ENABLED, EndpointExposure.JMX,
				"endpoints.bar.web.enabled=false");
		validate(enablement, true, "endpoint 'bar' (jmx) is enabled by default");
	}

	@Test
	public void defaultEnablementNeutral() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL);
		validate(enablement, true, "endpoint 'biz' is enabled (default)");
	}

	@Test
	public void defaultEnablementNeutralWeb() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.WEB);
		validate(enablement, false, "endpoint 'default' (web) is disabled by default");
	}

	@Test
	public void defaultEnablementNeutralJmx() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX);
		validate(enablement, true,
				"endpoint 'biz' (jmx) is enabled (default for jmx endpoints)");
	}

	@Test
	public void defaultEnablementNeutralWithGeneralDisablement() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, "endpoints.default.enabled=false");
		validate(enablement, false, "found property endpoints.default.enabled");
	}

	@Test
	public void defaultEnablementNeutralWebWithTechDisablement() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.jmx.enabled=false");
		validate(enablement, false, "found property endpoints.default.jmx.enabled");
	}

	@Test
	public void defaultEnablementNeutralTechTakesPrecedence() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.enabled=true", "endpoints.default.jmx.enabled=false");
		validate(enablement, false, "found property endpoints.default.jmx.enabled");
	}

	@Test
	public void defaultEnablementNeutralWebWithTechEnablement() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.WEB,
				"endpoints.default.web.enabled=true");
		validate(enablement, true, "found property endpoints.default.web.enabled");
	}

	@Test
	public void defaultEnablementNeutralWebWithUnrelatedTechDisablement() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.web.enabled=false");
		validate(enablement, true,
				"endpoint 'biz' (jmx) is enabled (default for jmx endpoints)");
	}

	@Test
	public void defaultEnablementNeutralWithOverride() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, "endpoints.biz.enabled=false");
		validate(enablement, false, "found property endpoints.biz.enabled");
	}

	@Test
	public void defaultEnablementNeutralWebWithOverride() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.WEB,
				"endpoints.biz.web.enabled=true");
		validate(enablement, true, "found property endpoints.biz.web.enabled");
	}

	@Test
	public void defaultEnablementNeutralJmxWithOverride() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.biz.jmx.enabled=false");
		validate(enablement, false, "found property endpoints.biz.jmx.enabled");
	}

	@Test
	public void defaultEnablementNeutralTechTakesPrecedenceOnEverything() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.enabled=false", "endpoints.default.jmx.enabled=false",
				"endpoints.biz.enabled=false", "endpoints.biz.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.biz.jmx.enabled");
	}

	@Test
	public void defaultEnablementNeutralSpecificTakesPrecedenceOnDefaults() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.enabled=false", "endpoints.default.jmx.enabled=false",
				"endpoints.biz.enabled=true");
		validate(enablement, true, "found property endpoints.biz.enabled");
	}

	@Test
	public void defaultEnablementNeutralDefaultTechTakesPrecedenceOnGeneralDefault() {
		EndpointEnablement enablement = getEndpointEnablement("biz",
				DefaultEnablement.NEUTRAL, EndpointExposure.JMX,
				"endpoints.default.enabled=false", "endpoints.default.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.default.jmx.enabled");
	}

	private EndpointEnablement getEndpointEnablement(String id,
			DefaultEnablement enabledByDefault, String... environment) {
		return getEndpointEnablement(id, enabledByDefault, null, environment);
	}

	private EndpointEnablement getEndpointEnablement(String id,
			DefaultEnablement enabledByDefault, EndpointExposure exposure,
			String... environment) {
		MockEnvironment env = new MockEnvironment();
		TestPropertyValues.of(environment).applyTo(env);
		EndpointEnablementProvider provider = new EndpointEnablementProvider(env);
		if (exposure != null) {
			return provider.getEndpointEnablement(id, enabledByDefault, exposure);
		}
		return provider.getEndpointEnablement(id, enabledByDefault);
	}

	private void validate(EndpointEnablement enablement, boolean enabled,
			String... messages) {
		assertThat(enablement).isNotNull();
		assertThat(enablement.isEnabled()).isEqualTo(enabled);
		if (!ObjectUtils.isEmpty(messages)) {
			assertThat(enablement.getReason()).contains(messages);
		}
	}

}
