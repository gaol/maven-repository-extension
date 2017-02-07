/*
 * JBoss, Home of Professional Open Source
 * Copyright @year, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.maven.extensions.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.Installer;
import org.eclipse.aether.impl.LocalRepositoryProvider;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.log.LoggerFactory;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
@Component(role = RepositorySystem.class)
public class RedHatRepositorySystem extends DefaultRepositorySystem {

    private static final String REPO_EXTENSTION_ENABLED = "repo.extension.enabled";

    private static final String DEBUG = "debug";

    private static final String REDHAT_REPO_STR = "redhat";

    private static final String REPO_EXTENSTION_ENABLED_MESSAGE = "Red Hat Maven Repository Extenstion is loaded.";

    private boolean enabled = Boolean.getBoolean(REPO_EXTENSTION_ENABLED);
    private boolean debug = Boolean.getBoolean(DEBUG);

    public RedHatRepositorySystem(){
        // default constructor.
    }

    @Inject
    RedHatRepositorySystem(VersionResolver versionResolver, VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver, MetadataResolver metadataResolver,
            ArtifactDescriptorReader artifactDescriptorReader, DependencyCollector dependencyCollector, Installer installer,
            Deployer deployer, LocalRepositoryProvider localRepositoryProvider, SyncContextFactory syncContextFactory,
            RemoteRepositoryManager remoteRepositoryManager, LoggerFactory loggerFactory) {
        super();
        setVersionResolver(versionResolver);
        setVersionRangeResolver(versionRangeResolver);
        setArtifactResolver(artifactResolver);
        setMetadataResolver(metadataResolver);
        setArtifactDescriptorReader(artifactDescriptorReader);
        setDependencyCollector(dependencyCollector);
        setInstaller(installer);
        setDeployer(deployer);
        setLocalRepositoryProvider(localRepositoryProvider);
        setSyncContextFactory(syncContextFactory);
        setRemoteRepositoryManager(remoteRepositoryManager);
        setLoggerFactory(loggerFactory);
        logLoadedMessage();
    }

    private void logLoadedMessage() {
        if (enabled) {
            info(REPO_EXTENSTION_ENABLED_MESSAGE);
        }
    }

    private void debug(String message) {
        if (debug) {
            System.out.println("[DEBUG] " + message);
        }
    }

    private void info(String message) {
        System.out.println("[INFO] " + message);
    }

    private void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    List<RemoteRepository> getRemoteRepositories(Artifact artifact, List<RemoteRepository> candidates) {
        List<RemoteRepository> repos = new ArrayList<RemoteRepository>();
        for (RemoteRepository repo: candidates) {
            if (artifact != null && artifact.getVersion().contains(REDHAT_REPO_STR)) {
                if (repo.getUrl().contains(REDHAT_REPO_STR)) {
                    debug("Use Repo: " + repo.getUrl() + " for artifact: " + artifact);
                    repos.add(repo);
                }
            } else {
                repos.add(repo);
            }
        }
        if (repos.isEmpty()) {
            error("No repository can be used for artifact: " + artifact);
            return repos;
        }

        Collections.sort(repos, new Comparator<RemoteRepository>() {
            @Override
            public int compare(RemoteRepository r1, RemoteRepository r2) {
                if (r1 != null && r2 == null) {
                    return 1;
                }
                if (r1 == null && r2 != null) {
                    return -1;
                }
                if (r1 == null && r2 == null) {
                    return 0;
                }
                if (r1.getUrl().contains(REDHAT_REPO_STR) && !r2.getUrl().contains(REDHAT_REPO_STR)) {
                    return 1;
                }
                if (!r1.getUrl().contains(REDHAT_REPO_STR) && r2.getUrl().contains(REDHAT_REPO_STR)) {
                    return -1;
                }
                return r1.getId().compareTo(r2.getId()); // compare by id alphabetic
            }
        });
        if (debug) {
            debug("Ordered repositores are:");
            for (RemoteRepository repo: repos) {
                debug(repo.getUrl());
            }
        }
        return repos;
    }

    @Override
    public DependencyResult resolveDependencies(RepositorySystemSession session, DependencyRequest request)
            throws DependencyResolutionException {
        if (enabled) {
            CollectRequest collectRequest = request.getCollectRequest();
            List<RemoteRepository> allRepos = collectRequest.getRepositories();
            for (Dependency dep: collectRequest.getDependencies()) {
                List<RemoteRepository> repos = getRemoteRepositories(dep.getArtifact(), allRepos);
                collectRequest.setRepositories(repos); // get ordered or limited.
                if (repos.size() == allRepos.size()) {
                    break; // try all repos
                }
            }
        }
        return super.resolveDependencies(session, request);
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(RepositorySystemSession session, ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        if (enabled) {
            request.setRepositories(getRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.readArtifactDescriptor(session, request);
    }

    @Override
    public CollectResult collectDependencies(RepositorySystemSession session, CollectRequest request)
            throws DependencyCollectionException {
        if (enabled) {
            List<RemoteRepository> allRepos = request.getRepositories();
            for (Dependency dep: request.getDependencies()) {
                List<RemoteRepository> repos = getRemoteRepositories(dep.getArtifact(), allRepos);
                request.setRepositories(repos); // get ordered or limited.
                if (repos.size() == allRepos.size()) {
                    break; // try all repos
                }
            }
        }
        return super.collectDependencies(session, request);
    }

    @Override
    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request)
            throws ArtifactResolutionException {
        if (enabled) {
            request.setRepositories(getRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveArtifact(session, request);
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {
        if (enabled) {
            for (ArtifactRequest request: requests) {
                request.setRepositories(getRemoteRepositories(request.getArtifact(), request.getRepositories()));
            }
        }
        return super.resolveArtifacts(session, requests);
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request)
            throws VersionResolutionException {
        if (enabled) {
            request.setRepositories(getRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveVersion(session, request);
    }

    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        if (enabled) {
            request.setRepositories(getRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveVersionRange(session, request);
    }

}
