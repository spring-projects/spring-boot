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

package org.springframework.boot.actuate.autoconfigure.endpoint.support;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.endpoint.EndpointExposure;
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

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void cannotDetermineEnablementWithEmptyEndpoint() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint id must have a value");
		getEndpointEnablement("   ", true);
	}

	@Test
	public void cannotDetermineEnablementOfEndpointAll() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Endpoint id 'default' is a reserved value and cannot "
				+ "be used by an endpoint");
		getEndpointEnablement("default", true);
	}

	@Test
	public void generalEnabledByDefault() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true);
		validate(enablement, true, "endpoint 'foo' is enabled by default");
	}

	@Test
	public void generalDisabledViaSpecificProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.foo.enabled=false");
		validate(enablement, false, "found property endpoints.foo.enabled");
	}

	@Test
	public void generalDisabledViaGeneralProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=false");
		validate(enablement, false, "found property endpoints.default.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=false", "endpoints.foo.enabled=true");
		validate(enablement, true, "found property endpoints.foo.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.foo.enabled=false", "endpoints.foo.web.enabled=true");
		validate(enablement, true, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificJmxProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.foo.enabled=false", "endpoints.foo.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.foo.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaSpecificAnyProperty() {
		validate(getEndpointEnablement("foo", true, "endpoints.foo.enabled=false",
				"endpoints.foo.web.enabled=false", "endpoints.foo.jmx.enabled=true"),
				true, "found property endpoints.foo.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=false", "endpoints.default.web.enabled=true");
		validate(enablement, true, "found property endpoints.default.web.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralJmxProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=false", "endpoints.default.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.default.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideViaGeneralAnyProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=false", "endpoints.default.web.enabled=false",
				"endpoints.default.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.default.jmx.enabled");
	}

	@Test
	public void generalDisabledEvenWithEnabledGeneralProperties() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				"endpoints.default.enabled=true", "endpoints.default.web.enabled=true",
				"endpoints.default.jmx.enabled=true", "endpoints.foo.enabled=false");
		validate(enablement, false, "found property endpoints.foo.enabled");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlag() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false);
		validate(enablement, false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.default.enabled=true");
		validate(enablement, false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.default.web.enabled=true");
		validate(enablement, false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalDisabledByDefaultWithAnnotationFlagEvenWithGeneralJmxProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.default.jmx.enabled=true");
		validate(enablement, false, "endpoint 'bar' is disabled by default");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.bar.enabled=true");
		validate(enablement, true, "found property endpoints.bar.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.bar.web.enabled=true");
		validate(enablement, true, "found property endpoints.bar.web.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndSpecificJmxProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.bar.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.bar.jmx.enabled");
	}

	@Test
	public void generalEnabledOverrideWithAndAnnotationFlagAndAnyProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				"endpoints.bar.web.enabled=false", "endpoints.bar.jmx.enabled=true");
		validate(enablement, true, "found property endpoints.bar.jmx.enabled");
	}

	@Test
	public void specificEnabledByDefault() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.JMX);
		validate(enablement, true, "endpoint 'foo' (jmx) is enabled by default");
	}

	@Test
	public void specificDisabledViaEndpointProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.foo.enabled=false");
		validate(enablement, false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificDisabledViaTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.foo.web.enabled=false");
		validate(enablement, false, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificNotDisabledViaUnrelatedTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.JMX, "endpoints.foo.web.enabled=false");
		validate(enablement, true, "endpoint 'foo' (jmx) is enabled by default");
	}

	@Test
	public void specificDisabledViaGeneralProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.JMX, "endpoints.default.enabled=false");
		validate(enablement, false, "found property endpoints.default.enabled");
	}

	@Test
	public void specificEnabledOverrideViaEndpointProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.default.enabled=false",
				"endpoints.foo.enabled=true");
		validate(enablement, true, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificEnabledOverrideViaTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.foo.enabled=false",
				"endpoints.foo.web.enabled=true");
		validate(enablement, true, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificEnabledOverrideHasNotEffectWithUnrelatedTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.foo.enabled=false",
				"endpoints.foo.jmx.enabled=true");
		validate(enablement, false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificEnabledOverrideViaGeneralWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.default.enabled=false",
				"endpoints.default.web.enabled=true");
		validate(enablement, true, "found property endpoints.default.web.enabled");
	}

	@Test
	public void specificEnabledOverrideHasNoEffectWithUnrelatedTechProperty() {
		validate(getEndpointEnablement("foo", true, EndpointExposure.JMX,
				"endpoints.default.enabled=false", "endpoints.default.web.enabled=true"),
				false, "found property endpoints.default.enabled");
	}

	@Test
	public void specificDisabledWithEndpointPropertyEvenWithEnabledGeneralProperties() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.default.enabled=true",
				"endpoints.default.web.enabled=true",
				"endpoints.default.jmx.enabled=true", "endpoints.foo.enabled=false");
		validate(enablement, false, "found property endpoints.foo.enabled");
	}

	@Test
	public void specificDisabledWithTechPropertyEvenWithEnabledGeneralProperties() {
		EndpointEnablement enablement = getEndpointEnablement("foo", true,
				EndpointExposure.WEB, "endpoints.default.enabled=true",
				"endpoints.default.web.enabled=true",
				"endpoints.default.jmx.enabled=true", "endpoints.foo.enabled=true",
				"endpoints.foo.web.enabled=false");
		validate(enablement, false, "found property endpoints.foo.web.enabled");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlag() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB);
		validate(enablement, false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.default.enabled=true");
		validate(enablement, false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralWebProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.default.web.enabled=true");
		validate(enablement, false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificDisabledByDefaultWithAnnotationFlagEvenWithGeneralJmxProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.default.jmx.enabled=true");
		validate(enablement, false, "endpoint 'bar' (web) is disabled by default");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagAndEndpointProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.bar.enabled=true");
		validate(enablement, true, "found property endpoints.bar.enabled");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagAndTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.bar.web.enabled=true");
		validate(enablement, true, "found property endpoints.bar.web.enabled");
	}

	@Test
	public void specificEnabledOverrideWithAndAnnotationFlagHasNoEffectWithUnrelatedTechProperty() {
		EndpointEnablement enablement = getEndpointEnablement("bar", false,
				EndpointExposure.WEB, "endpoints.bar.jmx.enabled=true");
		validate(enablement, false, "endpoint 'bar' (web) is disabled by default");
	}

	private EndpointEnablement getEndpointEnablement(String id, boolean enabledByDefault,
			String... environment) {
		return getEndpointEnablement(id, enabledByDefault, null, environment);
	}

	private EndpointEnablement getEndpointEnablement(String id, boolean enabledByDefault,
			EndpointExposure exposure, String... environment) {
		MockEnvironment env = new MockEnvironment();
		TestPropertyValues.of(environment).applyTo(env);
		EndpointEnablementProvider provider = new EndpointEnablementProvider(env);
		return provider.getEndpointEnablement(id, enabledByDefault, exposure);
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
