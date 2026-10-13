/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

final class SpringApplicationAdmin implements SpringApplicationAdminMXBean {

	boolean ready;

	boolean shutdownInvoked;

	@Override
	public boolean isReady() {
		System.out.println("isReady: " + this.ready);
		return this.ready;
	}

	@Override
	public void shutdown() {
		this.shutdownInvoked = true;
		System.out.println("Shutdown requested");
	}

}