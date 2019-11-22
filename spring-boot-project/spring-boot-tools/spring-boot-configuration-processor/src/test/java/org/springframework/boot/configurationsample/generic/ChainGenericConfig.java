package org.springframework.boot.configurationsample.generic;

/**
 * Chain Generic
 *
 * @author L.cm
 * @param <T> name type
 */
public class ChainGenericConfig<T extends ChainGenericConfig> {

	private Integer pingTimeout = 1000;

	public int getPingTimeout() {
		return pingTimeout;
	}

	public T setPingTimeout(int pingTimeout) {
		this.pingTimeout = pingTimeout;
		return (T) this;
	}
}
