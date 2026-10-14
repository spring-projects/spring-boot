/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.ToolchainManager;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.tools.RunProcess;

/**
 * Start a spring application. Contrary to the {@code run} goal, this does not block and
 * allows other goals to operate on the application. This goal is typically used in
 * integration test scenario where the application is started before a test suite and
 * stopped after.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see StopMojo
 */
@Mojo(name = "start", requiresProject = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
		requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractRunMojo {

	private static final String ENABLE_MBEAN_PROPERTY = "--spring.application.admin.enabled=true";

	private static final String JMX_NAME_PROPERTY_PREFIX = "--spring.application.admin.jmx-name=";

	/**
	 * The JMX name of the automatically deployed MBean managing the lifecycle of the
	 * spring application.
	 */
	@Parameter(defaultValue = SpringApplicationAdminClient.DEFAULT_OBJECT_NAME)
	@SuppressWarnings("NullAway.Init")
	private String jmxName;

	/**
	 * The port to use to expose the platform MBeanServer.
	 */
	@Parameter(defaultValue = "9001")
	private int jmxPort;

	/**
	 * The number of milliseconds to wait between each attempt to check if the spring
	 * application is ready.
	 */
	@Parameter(property = "spring-boot.start.wait", defaultValue = "500")
	private long wait;

	/**
	 * The maximum number of attempts to check if the spring application is ready.
	 * Combined with the "wait" argument, this gives a global timeout value (30 sec by
	 * default)
	 */
	@Parameter(property = "spring-boot.start.maxAttempts", defaultValue = "60")
	private int maxAttempts;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	@SuppressWarnings("NullAway.Init")
	private String buildDirectory;

	private final Object lock = new Object();

	/**
	 * Flag to include the test classpath when running.
	 */
	@Parameter(property = "spring-boot.run.useTestClasspath", defaultValue = "false")
	private boolean useTestClasspath;

	private final String password = UUID.randomUUID().toString();

	private final boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

	@Inject
	public StartMojo(ToolchainManager toolchainManager) {
		super(toolchainManager);
	}

	@Override
	protected void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
			Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException {
		RunProcess runProcess = processExecutor.runAsync(workingDirectory, args, environmentVariables);
		try {
			waitForSpringApplication();
		}
		catch (MojoExecutionException | MojoFailureException ex) {
			runProcess.kill();
			throw ex;
		}
	}

	@Override
	protected RunArguments resolveApplicationArguments() {
		RunArguments applicationArguments = super.resolveApplicationArguments();
		applicationArguments.getArgs().addLast(ENABLE_MBEAN_PROPERTY);
		applicationArguments.getArgs().addLast(JMX_NAME_PROPERTY_PREFIX + this.jmxName);
		return applicationArguments;
	}

	@Override
	protected RunArguments resolveJvmArguments() {
		RunArguments jvmArguments = super.resolveJvmArguments();
		List<String> remoteJmxArguments = new ArrayList<>();
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.port=" + this.jmxPort);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.rmi.port=" + this.jmxPort);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.local.only=true");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.host=127.0.0.1");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.authenticate=true");
		Path passwordFile = new File(this.buildDirectory, "org.springframework.boot.jmxremote.password").toPath();
		writeWithOwnerOnlyPermissions(passwordFile, "maven-build " + this.password);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.password.file=" + passwordFile);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.password.toHashes=false");
		Path accessFile = new File(this.buildDirectory, "org.springframework.boot.jmxremote.access").toPath();
		writeWithOwnerOnlyPermissions(accessFile, "maven-build readwrite");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.access.file=" + accessFile);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.ssl=false");
		remoteJmxArguments.add("-Djava.rmi.server.hostname=127.0.0.1");
		jvmArguments.getArgs().addAll(remoteJmxArguments);
		return jvmArguments;
	}

	private void writeWithOwnerOnlyPermissions(Path file, String content) {
		try {
			Files.deleteIfExists(file);
			if (this.posix) {
				Files.createFile(file, PosixFilePermissions
					.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
			}
			else {
				Files.createFile(file);
				AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView.class);
				if (view != null) {
					UserPrincipal owner = view.getOwner();
					AclEntry ownerOnly = AclEntry.newBuilder()
						.setType(AclEntryType.ALLOW)
						.setPrincipal(owner)
						.setPermissions(EnumSet.allOf(AclEntryPermission.class))
						.build();
					view.setAcl(Collections.singletonList(ownerOnly));
				}
				else {
					file.toFile().setReadable(false, false); // Remove read permissions
					file.toFile().setReadable(true); // Allow owner read
					file.toFile().setWritable(false, false); // Remove write permissions
					file.toFile().setWritable(true); // Allow owner write
				}
			}
			Files.write(file, Collections.singletonList(content));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void waitForSpringApplication() throws MojoFailureException, MojoExecutionException {
		try {
			getLog().debug("Connecting to local MBeanServer at port " + this.jmxPort);
			try (JMXConnector connector = execute(this.wait, this.maxAttempts,
					new CreateJmxConnector(this.jmxPort, this.password))) {
				if (connector == null) {
					throw new MojoExecutionException("JMX MBean server was not reachable before the configured "
							+ "timeout (" + (this.wait * this.maxAttempts) + "ms");
				}
				getLog().debug("Connected to local MBeanServer at port " + this.jmxPort);
				MBeanServerConnection connection = connector.getMBeanServerConnection();
				doWaitForSpringApplication(connection);
			}
		}
		catch (IOException ex) {
			throw new MojoFailureException("Could not contact Spring Boot application via JMX on port " + this.jmxPort
					+ ". Please make sure that no other process is using that port", ex);
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Failed to connect to MBean server at port " + this.jmxPort, ex);
		}
	}

	private void doWaitForSpringApplication(MBeanServerConnection connection)
			throws MojoExecutionException, MojoFailureException {
		final SpringApplicationAdminClient client = new SpringApplicationAdminClient(connection, this.jmxName);
		try {
			Callable<@Nullable Boolean> isReady = () -> (client.isReady() ? true : null);
			execute(this.wait, this.maxAttempts, isReady);
		}
		catch (ReflectionException ex) {
			throw new MojoExecutionException("Unable to retrieve 'ready' attribute", ex.getCause());
		}
		catch (Exception ex) {
			throw new MojoFailureException("Could not invoke shutdown operation", ex);
		}
	}

	/**
	 * Execute a task, retrying it on failure.
	 * @param <T> the result type
	 * @param wait the wait time
	 * @param maxAttempts the maximum number of attempts
	 * @param callback the task to execute (possibly multiple times). The callback should
	 * return {@code null} to indicate that another attempt should be made
	 * @return the result
	 * @throws Exception in case of execution errors
	 */
	public <T> T execute(long wait, int maxAttempts, Callable<@Nullable T> callback) throws Exception {
		getLog().debug("Waiting for spring application to start...");
		for (int i = 0; i < maxAttempts; i++) {
			T result = callback.call();
			if (result != null) {
				return result;
			}
			String message = "Spring application is not ready yet, waiting " + wait + "ms (attempt " + (i + 1) + ")";
			getLog().debug(message);
			synchronized (this.lock) {
				try {
					this.lock.wait(wait);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while waiting for Spring Boot app to start.");
				}
			}
		}
		throw new MojoExecutionException(
				"Spring application did not start before the configured timeout (" + (wait * maxAttempts) + "ms");
	}

	@Override
	protected boolean isUseTestClasspath() {
		return this.useTestClasspath;
	}

	private class CreateJmxConnector implements Callable<@Nullable JMXConnector> {

		private final int port;

		private final String password;

		CreateJmxConnector(int port, String password) {
			this.port = port;
			this.password = password;
		}

		@Override
		public @Nullable JMXConnector call() throws Exception {
			try {
				return SpringApplicationAdminClient.connect(this.port, this.password);
			}
			catch (IOException ex) {
				if (hasCauseWithType(ex, ConnectException.class)) {
					String message = "MBean server at port " + this.port + " is not up yet...";
					getLog().debug(message);
					return null;
				}
				throw ex;
			}
		}

		private boolean hasCauseWithType(Throwable t, Class<? extends Exception> type) {
			return type.isInstance(t) || t.getCause() != null && hasCauseWithType(t.getCause(), type);
		}

	}

}
