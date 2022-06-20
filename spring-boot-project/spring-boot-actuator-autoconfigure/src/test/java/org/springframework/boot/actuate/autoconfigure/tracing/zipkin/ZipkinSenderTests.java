package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import zipkin2.Callback;
import zipkin2.reporter.Sender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class which is used for testing the different implementations of the
 * Zipkin {@link Sender}.
 *
 * @author Stefan Bratanov
 */
abstract class ZipkinSenderTests {

	protected Sender senderUnderTest;

	abstract Sender getZipkinSender();

	@BeforeEach
	void setUp() {
		this.senderUnderTest = getZipkinSender();
	}

	protected void makeRequest(List<byte[]> encodedSpans, boolean async) {
		if (async) {
			CallbackResult callbackResult = this.makeAsyncRequest(encodedSpans);
			assertThat(callbackResult.isSuccess()).isTrue();
		}
		else {
			try {
				this.makeSyncRequest(encodedSpans);
			}
			catch (IOException ex) {
				Assertions.fail(ex);
			}
		}
	}

	protected CallbackResult makeAsyncRequest(List<byte[]> encodedSpans) {
		AtomicReference<CallbackResult> callbackResult = new AtomicReference<>();
		this.senderUnderTest.sendSpans(encodedSpans).enqueue(new Callback<>() {
			@Override
			public void onSuccess(Void value) {
				callbackResult.set(new CallbackResult(true, null));
			}

			@Override
			public void onError(Throwable t) {
				callbackResult.set(new CallbackResult(false, t));
			}
		});
		return Awaitility.await().atMost(Duration.ofSeconds(5)).until(callbackResult::get, Objects::nonNull);
	}

	protected void makeSyncRequest(List<byte[]> encodedSpans) throws IOException {
		this.senderUnderTest.sendSpans(encodedSpans).execute();
	}

	protected byte[] toByteArray(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

	protected static final class CallbackResult {

		private final boolean isSuccess;

		private final Throwable error;

		private CallbackResult(boolean isSuccess, Throwable error) {
			this.isSuccess = isSuccess;
			this.error = error;
		}

		public boolean isSuccess() {
			return isSuccess;
		}

		public Throwable getError() {
			return error;
		}

	}

}
