/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.data.redis;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleRedisApplication}.
 *
 * @author Dave Syer
 */
public class SampleRedisApplicationTests {

	@Rule
	public final OutputCapture output = new OutputCapture();

	@Test
	public void testDefaultSettings() {
		try {
			SampleRedisApplication.main(new String[0]);
		}
		catch (Exception ex) {
			if (!redisServerRunning(ex)) {
				return;
			}
		}
		assertThat(this.output.toString()).contains("Found key spring.boot.redis.test");
	}

	private boolean redisServerRunning(Throwable ex) {
		System.out.println(ex.getMessage());
		if (ex instanceof RedisConnectionFailureException) {
			return false;
		}
		return (ex.getCause() == null || redisServerRunning(ex.getCause()));
	}

}
