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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

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
		assertThat(JavaVersion.SEVENTEEN.compareTo(JavaVersion.EIGHTEEN)).isLessThan(0);
	}

	@Test
	void compareToWhenComparingGreaterToSmallerShouldBeGreaterThanZero() {
		assertThat(JavaVersion.EIGHTEEN.compareTo(JavaVersion.SEVENTEEN)).isGreaterThan(0);
	}

	@Test
	void compareToWhenComparingSameShouldBeZero() {
		assertThat(JavaVersion.SEVENTEEN.compareTo(JavaVersion.SEVENTEEN)).isEqualTo(0);
	}

	@Test
	void isEqualOrNewerThanWhenComparingSameShouldBeTrue() {
		assertThat(JavaVersion.SEVENTEEN.isEqualOrNewerThan(JavaVersion.SEVENTEEN)).isTrue();
	}

	@Test
	void isEqualOrNewerThanWhenSmallerToGreaterShouldBeFalse() {
		assertThat(JavaVersion.SEVENTEEN.isEqualOrNewerThan(JavaVersion.EIGHTEEN)).isFalse();
	}

	@Test
	void isEqualOrNewerThanWhenGreaterToSmallerShouldBeTrue() {
		assertThat(JavaVersion.EIGHTEEN.isEqualOrNewerThan(JavaVersion.SEVENTEEN)).isTrue();
	}

	@Test
	void isOlderThanThanWhenComparingSameShouldBeFalse() {
		assertThat(JavaVersion.SEVENTEEN.isOlderThan(JavaVersion.SEVENTEEN)).isFalse();
	}

	@Test
	void isOlderThanWhenSmallerToGreaterShouldBeTrue() {
		assertThat(JavaVersion.SEVENTEEN.isOlderThan(JavaVersion.EIGHTEEN)).isTrue();
	}

	@Test
	void isOlderThanWhenGreaterToSmallerShouldBeFalse() {
		assertThat(JavaVersion.EIGHTEEN.isOlderThan(JavaVersion.SEVENTEEN)).isFalse();
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void currentJavaVersionSeventeen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.SEVENTEEN);
	}

	@Test
	@EnabledOnJre(JRE.JAVA_18)
	void currentJavaVersionEighteen() {
		assertThat(JavaVersion.getJavaVersion()).isEqualTo(JavaVersion.EIGHTEEN);
	}

}
