/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.Certificate;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.CertificateSelector;
import org.springframework.util.Assert;

/**
 * {@link CertificateSelector} implementations.
 *
 * @author Moritz Halbritter
 */
final class CertificateSelectors {

	private CertificateSelectors() {
	}

	abstract static class AbstractCertificateSelector implements CertificateSelector {

		private final Clock clock;

		AbstractCertificateSelector() {
			this(Clock.systemDefaultZone());
		}

		AbstractCertificateSelector(Clock clock) {
			Assert.notNull(clock, "clock must not be null");
			this.clock = clock;
		}

		@Override
		public Certificate select(List<Certificate> certificates) {
			Instant now = this.clock.instant();
			List<Certificate> preProcessed = certificates.stream().filter((c) -> isUsable(c, now)).toList();
			return doSelect(preProcessed);
		}

		protected abstract Certificate doSelect(List<Certificate> candidates);

		private boolean isUsable(Certificate certificate, Instant now) {
			return now.isAfter(certificate.certificate().getNotBefore().toInstant())
					&& now.isBefore(certificate.certificate().getNotAfter().toInstant());
		}

	}

	static class MaximumNotAfterCertificateSelector extends AbstractCertificateSelector {

		MaximumNotAfterCertificateSelector() {
			super();
		}

		MaximumNotAfterCertificateSelector(Clock clock) {
			super(clock);
		}

		@Override
		protected Certificate doSelect(List<Certificate> candidates) {
			return candidates.stream().max(Comparator.comparing((c) -> c.certificate().getNotAfter())).orElse(null);
		}

	}

	static class MaximumNotBeforeCertificateSelector extends AbstractCertificateSelector {

		MaximumNotBeforeCertificateSelector() {
			super();
		}

		MaximumNotBeforeCertificateSelector(Clock clock) {
			super(clock);
		}

		@Override
		protected Certificate doSelect(List<Certificate> candidates) {
			return candidates.stream().max(Comparator.comparing((c) -> c.certificate().getNotBefore())).orElse(null);
		}

	}

	static class NewestFileCertificateSelector extends AbstractCertificateSelector {

		NewestFileCertificateSelector() {
			super();
		}

		NewestFileCertificateSelector(Clock clock) {
			super(clock);
		}

		@Override
		protected Certificate doSelect(List<Certificate> candidates) {
			if (candidates.isEmpty()) {
				return null;
			}
			if (candidates.size() == 1) {
				return candidates.get(0);
			}
			Certificate certificate = null;
			Instant created = null;
			for (Certificate candidate : candidates) {
				BasicFileAttributes attributes;
				try {
					attributes = Files.readAttributes(candidate.file(), BasicFileAttributes.class);
				}
				catch (IOException ex) {
					throw new UncheckedIOException("Failed to get creation time of file %s".formatted(candidate.file()),
							ex);
				}
				Instant candidateCreationTime = attributes.creationTime().toInstant();
				if (created == null || candidateCreationTime.isAfter(created)) {
					certificate = candidate;
					created = candidateCreationTime;
				}
			}
			return certificate;
		}

	}

}
