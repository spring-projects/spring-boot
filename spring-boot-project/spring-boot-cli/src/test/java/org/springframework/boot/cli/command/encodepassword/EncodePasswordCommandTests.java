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

package org.springframework.boot.cli.command.encodepassword;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.MockLog;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EncodePasswordCommand}.
 *
 * @author Phillip Webb
 */
public class EncodePasswordCommandTests {

	private MockLog log;

	@Captor
	private ArgumentCaptor<String> message;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.log = MockLog.attach();
	}

	@After
	public void cleanup() {
		MockLog.clear();
	}

	@Test
	public void encodeWithNoAlgorithmShouldUseBcrypt() throws Exception {
		EncodePasswordCommand command = new EncodePasswordCommand();
		ExitStatus status = command.run("boot");
		verify(this.log).info(this.message.capture());
		assertThat(this.message.getValue()).startsWith("{bcrypt}");
		assertThat(PasswordEncoderFactories.createDelegatingPasswordEncoder()
				.matches("boot", this.message.getValue())).isTrue();
		assertThat(status).isEqualTo(ExitStatus.OK);
	}

	@Test
	public void encodeWithBCryptShouldUseBCrypt() throws Exception {
		EncodePasswordCommand command = new EncodePasswordCommand();
		ExitStatus status = command.run("-a", "bcrypt", "boot");
		verify(this.log).info(this.message.capture());
		assertThat(this.message.getValue()).doesNotStartWith("{");
		assertThat(new BCryptPasswordEncoder().matches("boot", this.message.getValue()))
				.isTrue();
		assertThat(status).isEqualTo(ExitStatus.OK);
	}

	@Test
	public void encodeWithPbkdf2ShouldUsePbkdf2() throws Exception {
		EncodePasswordCommand command = new EncodePasswordCommand();
		ExitStatus status = command.run("-a", "pbkdf2", "boot");
		verify(this.log).info(this.message.capture());
		assertThat(this.message.getValue()).doesNotStartWith("{");
		assertThat(new Pbkdf2PasswordEncoder().matches("boot", this.message.getValue()))
				.isTrue();
		assertThat(status).isEqualTo(ExitStatus.OK);
	}

	@Test
	public void encodeWithUnknownAlgorithmShouldExitWithError() throws Exception {
		EncodePasswordCommand command = new EncodePasswordCommand();
		ExitStatus status = command.run("--algorithm", "bad", "boot");
		verify(this.log)
				.error("Unknown algorithm, valid options are: default,bcrypt,pbkdf2");
		assertThat(status).isEqualTo(ExitStatus.ERROR);
	}

}
