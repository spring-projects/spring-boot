/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.geode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import sample.data.geode.config.SampleDataGeodeApplicationProperties;
import sample.data.geode.service.Calculator;

/**
 * Integration tests for {@link SampleDataGeodeApplication}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringRunner
 * @see sample.data.geode.SampleDataGeodeApplication
 * @since 1.5.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleDataGeodeApplication.class)
@SuppressWarnings("unused")
public class SampleDataGeodeApplicationTests {

	@Autowired
	@Qualifier("calculatorResolver")
	private Calculator<Long, Long> calculator;

	@Autowired
	private GemfireTemplate calculationsTemplate;

	@Autowired
	private SampleDataGeodeApplicationProperties applicationProperties;

	@Before
	public void setup() {
		assertThat(calculator).isNotNull();
		assertThat(calculationsTemplate).isNotNull();
		assertThat(applicationProperties).isNotNull();
		assertThat(calculator.toString()).isEqualTo(applicationProperties.getCalculator());
		System.err.printf("%1$s(%2$d) = %3$d%n", calculator.toString(), 0l, calculator.calculate(0l));
		System.err.printf("%1$s(%2$d) = %3$d%n", calculator.toString(), 2l, calculator.calculate(2l));
		System.err.printf("%1$s(%2$d) = %3$d%n", calculator.toString(), 4l, calculator.calculate(4l));
		System.err.printf("%1$s(%2$d) = %3$d%n", calculator.toString(), 8l, calculator.calculate(8l));
	}

	@Test
	public void verifyCalculationsAreCorrect() {
		assertThat(calculationsTemplate.<Long, Long>get(0l)).isEqualTo(calculator.calculate(0l));
		assertThat(calculationsTemplate.<Long, Long>get(1l)).isEqualTo(calculator.calculate(1l));
		assertThat(calculationsTemplate.<Long, Long>get(2l)).isEqualTo(calculator.calculate(2l));
		assertThat(calculationsTemplate.<Long, Long>get(3l)).isEqualTo(calculator.calculate(3l));
		assertThat(calculationsTemplate.<Long, Long>get(4l)).isEqualTo(calculator.calculate(4l));
		assertThat(calculationsTemplate.<Long, Long>get(5l)).isEqualTo(calculator.calculate(5l));
		assertThat(calculationsTemplate.<Long, Long>get(6l)).isEqualTo(calculator.calculate(6l));
		assertThat(calculationsTemplate.<Long, Long>get(7l)).isEqualTo(calculator.calculate(7l));
		assertThat(calculationsTemplate.<Long, Long>get(8l)).isEqualTo(calculator.calculate(8l));
		assertThat(calculationsTemplate.<Long, Long>get(9l)).isEqualTo(calculator.calculate(9l));
	}
}
