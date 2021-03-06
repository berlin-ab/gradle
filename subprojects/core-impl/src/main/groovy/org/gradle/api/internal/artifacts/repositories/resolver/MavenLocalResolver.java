/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.repositories.resolver;

import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.ResolverStrategy;
import org.gradle.api.internal.artifacts.metadata.MavenModuleVersionMetaData;
import org.gradle.api.internal.artifacts.metadata.ModuleVersionArtifactMetaData;
import org.gradle.api.internal.artifacts.metadata.MutableModuleVersionMetaData;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransport;
import org.gradle.api.internal.externalresource.local.LocallyAvailableResourceFinder;
import org.gradle.internal.filestore.FileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class MavenLocalResolver extends MavenResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolver.class);

    public MavenLocalResolver(String name, URI rootUri, RepositoryTransport transport,
                              LocallyAvailableResourceFinder<ModuleVersionArtifactMetaData> locallyAvailableResourceFinder,
                              ResolverStrategy resolverStrategy, FileStore<ModuleVersionArtifactMetaData> artifactFileStore) {
        super(name, rootUri, transport, locallyAvailableResourceFinder, resolverStrategy, artifactFileStore);
    }

    @Override
    protected MutableModuleVersionMetaData parseMetaDataFromArtifact(ModuleComponentIdentifier moduleComponentIdentifier, ExternalResourceArtifactResolver artifactResolver) {
        MutableModuleVersionMetaData metaData = super.parseMetaDataFromArtifact(moduleComponentIdentifier, artifactResolver);

        if (isOrphanedPom(mavenMetaData(metaData), artifactResolver)) {
            return null;
        }
        return metaData;
    }

    private boolean isOrphanedPom(MavenModuleVersionMetaData metaData, ExternalResourceArtifactResolver artifactResolver) {
        if (metaData.isPomPackaging()) {
            return false;
        }

        // check custom packaging
        if(!metaData.isKnownJarPackaging()) {
            ModuleVersionArtifactMetaData customArtifactMetaData = metaData.artifact(metaData.getPackaging(), metaData.getPackaging(), null);

            if(artifactResolver.artifactExists(customArtifactMetaData)) {
                return false;
            }
        }

        ModuleVersionArtifactMetaData artifact = metaData.artifact("jar", "jar", null);

        if(artifactResolver.artifactExists(artifact)) {
            return false;
        }

        LOGGER.debug("POM file found for module '{}' in repository '{}' but no artifact found. Ignoring.", metaData.getId(), getName());
        return true;
    }
}
