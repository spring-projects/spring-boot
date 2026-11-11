/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.bom.bomr;

import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.Library.LinkType;
import org.springframework.boot.build.properties.BuildProperties;

/**
 * {@link Task} to upgrade the libraries managed by a bom.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
public abstract class UpgradeBom extends UpgradeDependencies {

	@Inject
	public UpgradeBom(BomExtension bom) {
		super(bom);
		switch (BuildProperties.get(getProject()).buildType()) {
			case OPEN_SOURCE -> addOpenSourceRepositories(getProject().getRepositories());
			case COMMERCIAL -> addCommercialRepositories();
		}
	}

	private void addOpenSourceRepositories(RepositoryHandler repositories) {
		getRepositoryNames().add(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME);
		repositories.withType(MavenArtifactRepository.class, (repository) -> {
			String name = repository.getName();
			if (name.startsWith("spring-") && !name.endsWith("-snapshot")) {
				getRepositoryNames().add(name);
			}
		});
	}

	private void addCommercialRepositories() {
		getRepositoryNames().addAll(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME,
				"spring-commercial-release");
	}

	@Override
	protected String commitMessage(Upgrade upgrade, Integer issueNumber) {
		String message = issueTitle(upgrade);
		if (issueNumber != null) {
			message += "\n\nCloses gh-" + issueNumber;
		}
		return message;
	}

	@Override
	protected void upgradesApplied(List<Upgrade> upgrades) {
		if (upgrades.isEmpty()) {
			return;
		}
		System.out.println();
		System.out.println("Upgrade release notes:");
		System.out.println();
		for (Upgrade upgrade : upgrades) {
			Library library = upgrade.toRelease();
			String releaseNotesLinkUrl = library.getLinkUrl(LinkType.RELEASE_NOTES);
			System.out.println("* " + ((releaseNotesLinkUrl != null)
					? releaseNotesLinkUrl + "[" + library.getNameAndVersion() + "]" : library.getNameAndVersion()));
		}
		System.out.println();
	}

	@Override
	protected VersionResolver getVersionResolver() {
		String releaseTrain = getReleaseTrain().getOrNull();
		return (releaseTrain != null)
				? new ReleaseTrainVersionResolver(createGitHub("bomr.github.release-train-token"), releaseTrain)
				: super.getVersionResolver();
	}

}
