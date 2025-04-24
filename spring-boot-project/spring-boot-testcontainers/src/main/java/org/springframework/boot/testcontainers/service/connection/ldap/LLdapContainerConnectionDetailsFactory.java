/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.ldap;

import org.testcontainers.ldap.LLdapContainer;

import org.springframework.boot.autoconfigure.ldap.LdapConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link LdapConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link LLdapContainer}.
 *
 * @author Eddú Meléndez
 */
class LLdapContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LLdapContainer, LdapConnectionDetails> {

	@Override
	protected LdapConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LLdapContainer> source) {
		return new LLdapContainerConnectionDetails(source);
	}

	private static final class LLdapContainerConnectionDetails extends ContainerConnectionDetails<LLdapContainer>
			implements LdapConnectionDetails {

		private LLdapContainerConnectionDetails(ContainerConnectionSource<LLdapContainer> source) {
			super(source);
		}

		@Override
		public String[] getUrls() {
			return new String[] { getContainer().getLdapUrl() };
		}

		@Override
		public String getBase() {
			return getContainer().getBaseDn();
		}

		@Override
		public String getUsername() {
			return getContainer().getUser();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

	}

}
