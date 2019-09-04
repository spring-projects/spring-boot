/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.mail;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link HealthIndicator} for configured smtp server(s).
 *
 * @author Johannes Edmeier
 * @since 2.0.0
 */
public class MailHealthIndicator extends AbstractHealthIndicator {

	private final JavaMailSenderImpl mailSender;

	public MailHealthIndicator(JavaMailSenderImpl mailSender) {
		super("Mail health check failed");
		this.mailSender = mailSender;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		builder.withDetail("location", this.mailSender.getHost() + ":" + this.mailSender.getPort());
		this.mailSender.testConnection();
		builder.up();
	}

}
