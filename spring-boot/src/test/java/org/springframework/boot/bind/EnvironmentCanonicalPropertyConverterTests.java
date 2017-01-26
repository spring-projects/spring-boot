/*
 *
 *  * Copyright 2012-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.boot.bind;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Madhura Bhave
 */
@RunWith(Parameterized.class)
public class EnvironmentCanonicalPropertyConverterTests {

	private EnvironmentCanonicalPropertyConverter converter = new EnvironmentCanonicalPropertyConverter();

	@Parameter
	public String inputValue;

	@Parameter(1)
	public String expectedValue;

	@Parameters
	public static Object[] parameters() {
		return new Object[] {new Object[] {"FOO_BAR_BAZ", "foo.bar.baz" } ,
				new Object[] {"MY_FOO_1_", "my.foo[1]"  },
				new Object[] {"MY_FOO_1", "my.foo[1]"  },
				new Object[] {"MY_FOO_1_2_", "my.foo[1][2]" },
				new Object[] {"MY_FOO_1_2", "my.foo[1][2]" },
		};
	}

	@Test
	public void name() throws Exception {
		Assertions.assertThat(converter.convert(this.inputValue)).isEqualTo(this.expectedValue);
	}
}


