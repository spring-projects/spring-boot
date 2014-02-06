package org.springframework.boot.gradle;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;

/**
 * A resolution strategy to resolve missing version numbers using the
 * 'spring-boot-dependencies' POM.
 * 
 * @author Phillip Webb
 */
public class SpringBootResolutionStrategy {

	private static final String SPRING_BOOT_GROUP = "org.springframework.boot";

	public static void apply(ResolutionStrategy resolutionStrategy) {
		resolutionStrategy.eachDependency(new Action<DependencyResolveDetails>() {

			@Override
			public void execute(DependencyResolveDetails resolveDetails) {
				String version = resolveDetails.getTarget().getVersion();
				if (version == null || version.trim().length() == 0) {
					resolve(resolveDetails);
				}
			}

		});
	}

	protected static void resolve(DependencyResolveDetails resolveDetails) {

		ManagedDependencies dependencies = ManagedDependencies.get();
		ModuleVersionSelector target = resolveDetails.getTarget();

		if (SPRING_BOOT_GROUP.equals(target.getGroup())) {
			resolveDetails.useVersion(dependencies.getVersion());
			return;
		}

		Dependency dependency = dependencies.find(target.getGroup(), target.getName());
		if (dependency != null) {
			resolveDetails.useVersion(dependency.getVersion());
		}
	}

}
