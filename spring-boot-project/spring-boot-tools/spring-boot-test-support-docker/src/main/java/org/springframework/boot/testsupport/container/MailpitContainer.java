/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * A {@link GenericContainer} for MailDev.
 *
 * @author Rui Figueira
 */
public class MailpitContainer extends GenericContainer<MailpitContainer> {

	private static final int DEFAULT_SMTP_PORT = 1025;

	private static final int DEFAULT_POP3_PORT = 1110;

	public MailpitContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		addExposedPorts(DEFAULT_SMTP_PORT, DEFAULT_POP3_PORT);
	}

	public int getSmtpPort() {
		return getMappedPort(DEFAULT_SMTP_PORT);
	}

	public int getPop3Port() {
		return getMappedPort(DEFAULT_POP3_PORT);
	}

	public MailpitContainer withSmtpTlsCert(MountableFile cert) {
		this.withCopyFileToContainer(cert, "/tmp/ssl/public.crt");
		this.withEnv("MP_SMTP_TLS_CERT", "/tmp/ssl/public.crt");
		return this.self();
	}

	public MailpitContainer withSmtpTlsKey(MountableFile key) {
		this.withCopyFileToContainer(key, "/tmp/ssl/private.key");
		this.withEnv("MP_SMTP_TLS_KEY", "/tmp/ssl/private.key");
		return this.self();
	}

	public MailpitContainer withSmtpRequireTls(boolean requireTls) {
		if (requireTls) {
			this.withEnv("MP_SMTP_REQUIRE_TLS", "true");
		}
		return this.self();
	}

	public MailpitContainer withSmtpRequireStarttls(boolean requireStarttls) {
		if (requireStarttls) {
			this.withEnv("MP_SMTP_REQUIRE_STARTTLS", "true");
		}
		return this.self();
	}

	public MailpitContainer withPop3Auth(String... auths) {
		this.withEnv("MP_POP3_AUTH", String.join(" ", auths));
		return this.self();
	}

}
