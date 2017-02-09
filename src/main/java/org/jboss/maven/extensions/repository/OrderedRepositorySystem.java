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
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.jboss.maven.extensions.repository.OrderedRepositoryConfiguration.OrderRule;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
@Component(role = RepositorySystem.class)
public class OrderedRepositorySystem extends DefaultRepositorySystem {

    private static final String REPO_EXTENSTION_ENABLED = "ordered.repository.extension.enabled";
    private static final String ADDITIONAL_PROP_URL = "ordered.repository.extension.configure.url";

    static final String DEBUG = "ordered.repository.extension.debug";

    private static final String REPO_EXTENSTION_ENABLED_MESSAGE = "Ordered Maven Repository Extenstion is loaded.";
    
    private boolean enabled = Boolean.getBoolean(REPO_EXTENSTION_ENABLED);
    private boolean debug = Boolean.getBoolean(DEBUG);

    private OrderedRepositoryConfiguration config = null;

    public OrderedRepositorySystem(){
        // default constructor.
    }

    @Inject
    OrderedRepositorySystem(VersionResolver versionResolver, VersionRangeResolver versionRangeResolver,
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
        if (enabled) {
            config = new OrderedRepositoryConfiguration(System.getProperty(ADDITIONAL_PROP_URL));
        }
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

    private List<RemoteRepository> getOrderedRemoteRepositories(Artifact artifact, List<RemoteRepository> candidates) {
        String gav = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        List<RemoteRepository> repos = new ArrayList<RemoteRepository>();
        boolean matched = false;
        for (OrderRule rule: config.getOrderedRules()) {
            if (gav.matches(rule.getPattern())) {
                matched = true;
                // find matches, added repos in order of defined in rule.repos, then if include others, add left repos
                for (String repoId: rule.getRepos()) {
                    for (RemoteRepository repo: candidates) {
                        if (repoId.trim().equals(repo.getId().trim())) {
                            if (!repos.contains(repo)) {
                                repos.add(repo);
                            }
                        }
                    }
                }
                if (rule.isIncludeOtherRepo()) {
                    //add other repos back
                    for (RemoteRepository repo: candidates) {
                        if (!repos.contains(repo)) {
                            repos.add(repo);
                        }
                    }
                }
                break;
            }
        }
        if (!matched) {// try default repo orders
            for (String repoId: config.getDefaultRepos()) {
                for (RemoteRepository repo: candidates) {
                    if (repoId.trim().equals(repo.getId().trim())) {
                        if (!repos.contains(repo)) {
                            repos.add(repo);
                        }
                    }
                }
            }
            if (config.isReposIncludedByDefault()) {
                //add other repos back
                for (RemoteRepository repo: candidates) {
                    if (!repos.contains(repo)) {
                        repos.add(repo);
                    }
                }
            }
        }
        if (repos.isEmpty()) {
            error("No repository can be used for artifact: " + artifact);
            return repos;
        }

        if (debug) {
            debug("Ordered repositores for " + artifact + " are: ");
            for (RemoteRepository repo: repos) {
                debug(repo.getId() + ", url= " + repo.getUrl());
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
            debug("(resolveDependencies) Fix remote repositories for " + collectRequest.getDependencies());
            for (Dependency dep: collectRequest.getDependencies()) {
                List<RemoteRepository> repos = getOrderedRemoteRepositories(dep.getArtifact(), allRepos);
                collectRequest.setRepositories(repos); // get ordered or limited.
                if (repos.size() == allRepos.size()) {
                    break; // try all repos, but orderred already
                }
            }
        }
        return super.resolveDependencies(session, request);
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(RepositorySystemSession session, ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        if (enabled) {
            debug("(readArtifactDescriptor) Fix remote repositories for " + request.getArtifact());
            request.setRepositories(getOrderedRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.readArtifactDescriptor(session, request);
    }

    @Override
    public CollectResult collectDependencies(RepositorySystemSession session, CollectRequest request)
            throws DependencyCollectionException {
        if (enabled) {
            List<RemoteRepository> allRepos = request.getRepositories();
            debug("(collectDependencies) Fix remote repositories for " + request.getDependencies());
            for (Dependency dep: request.getDependencies()) {
                List<RemoteRepository> repos = getOrderedRemoteRepositories(dep.getArtifact(), allRepos);
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
            debug("(resolveArtifact) Fix remote repositories for " + request.getArtifact());
            request.setRepositories(getOrderedRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveArtifact(session, request);
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {
        if (enabled) {
            debug("(resolveArtifacts) Fix remote repositories for " + requests);
            for (ArtifactRequest request: requests) {
                request.setRepositories(getOrderedRemoteRepositories(request.getArtifact(), request.getRepositories()));
            }
        }
        return super.resolveArtifacts(session, requests);
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request)
            throws VersionResolutionException {
        if (enabled) {
            debug("(resolveVersion) Fix remote repositories for " + request.getArtifact());
            request.setRepositories(getOrderedRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveVersion(session, request);
    }

    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        if (enabled) {
            debug("(resolveVersionRange) Fix remote repositories for " + request.getArtifact());
            request.setRepositories(getOrderedRemoteRepositories(request.getArtifact(), request.getRepositories()));
        }
        return super.resolveVersionRange(session, request);
    }

}
