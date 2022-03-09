/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.system;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.platform.commons.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaVersion}.
 *
 * @author Stephane Nicoll
 */
class JavaVersionTests {

	@Test
	void getJavaVersionShouldBeAvailable() {
		assertThat(JavaVersion.getJavaVersion()).isNotNull();
	}

	@Test
	void compareToWhenComparingSmallerToGreaterShouldBeLessThanZero() {
		assertThat(JavaVersion.EIGHT.compareTo(JavaVersion.NINE)).isLessThan(0);
	}

	@Test
	void compareToWhenComparingGreaterToSmallerShouldBeGreaterThanZero() {
		assertThat(JavaVersion.NINE.compareTo(JavaVersion.EIGHT)).isGreaterThan(0);
	}

	@Test
	void compareToWhenComparingSameShouldBeZero() {
		assertThat(JavaVersion.EIGHT.compareTo(JavaVersion.EIGHT)).isEqualTo(0);
	}

	@Test
	void isEqualOrNewerThanWhenComparingSameShouldBeTrue() {
		assertThat(JavaVersion.EIGHT.isEqualOrNewerThan(JavaVersion.EIGHT)).isTrue();
	}

	@Test
	void isEqualOrNewerThanWhenSmallerToGreaterShouldBeFalse() {
		assertThat(JavaVersion.EIGHT.isEqualOrNewerThan(JavaVersion.NINE)).isFalse();
	}

	@Test
	void isEqualOrNewerThanWhenGreaterToSmallerShouldBeTrue() {
		assertThat(JavaVersion.NINE.isEqualOrNewerThan(JavaVersion.EIGHT)).isTrue();
	}

	@Test
	void isOlderThanThanWhenComparingSameShouldBeFalse() {
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.EIGHT)).isFalse();
	}

	@Test
	void isOlderThanWhenSmallerToGreaterShouldBeTrue() {
		assertThat(JavaVersion.EIGHT.isOlderThan(JavaVersion.NINE)).isTrue();
	}

	@Test
	void isOlderThanWhenGreaterToSmallerShouldBeFalse() {
		assertThat(JavaVersion.NINE.isOlderThan(JavaVersion.EIGHT)).isFalse();
	}

	@Test
	@EnabledOnJre(JRE.JAVA_8)
	void currentJavaVersionEight() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.EIGHT);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_9)
	void currentJavaVersionNine() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.NINE);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_10)
	void currentJavaVersionTen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.TEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_11)
	void currentJavaVersionEleven() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.ELEVEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_12)
	void currentJavaVersionTwelve() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.TWELVE);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_13)
	void currentJavaVersionThirteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.THIRTEEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_14)
	void currentJavaVersionFourteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.FOURTEEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_15)
	void currentJavaVersionFifteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.FIFTEEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_16)
	void currentJavaVersionSixteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.SIXTEEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void currentJavaVersionSeventeen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.SEVENTEEN);
	}

	@Test
	@EnabledIf("isJava18")
	void currentJavaVersionEighteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.EIGHTEEN);
	}

	boolean isJava18() throws Exception {
		// Same detection mechanism as org.junit.jupiter.api.condition.JRE
		// which does not have a JAVA_18 constant in JUnit 5.7.x
		try {
			Method versionMethod = Runtime.class.getMethod("version");
			Object version = ReflectionUtils.invokeMethod(versionMethod, null);
			Method majorMethod = version.getClass().getMethod("major");
			int major = (int) ReflectionUtils.invokeMethod(majorMethod, version);
			return major == 18;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
