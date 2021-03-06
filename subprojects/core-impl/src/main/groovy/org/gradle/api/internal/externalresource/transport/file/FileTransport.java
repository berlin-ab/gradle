/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.externalresource.transport.file;

import org.gradle.api.Nullable;
import org.gradle.api.internal.externalresource.LocallyAvailableExternalResource;
import org.gradle.api.internal.externalresource.local.LocallyAvailableResourceCandidates;
import org.gradle.api.internal.externalresource.transfer.CacheAwareExternalResourceAccessor;
import org.gradle.api.internal.externalresource.transport.AbstractRepositoryTransport;
import org.gradle.api.internal.externalresource.transport.DefaultExternalResourceRepository;
import org.gradle.api.internal.externalresource.transport.ExternalResourceRepository;
import org.gradle.api.internal.file.TemporaryFileProvider;

import java.io.IOException;
import java.net.URI;

public class FileTransport extends AbstractRepositoryTransport {
    private final ExternalResourceRepository repository;
    private final NoOpCacheAwareExternalResourceAccessor resourceAccessor;

    public FileTransport(String name, TemporaryFileProvider temporaryFileProvider) {
        super(name);
        FileResourceConnector connector = new FileResourceConnector();
        resourceAccessor = new NoOpCacheAwareExternalResourceAccessor(connector);
        repository = new DefaultExternalResourceRepository(name, connector, connector, connector, temporaryFileProvider);
    }

    public boolean isLocal() {
        return true;
    }

    public ExternalResourceRepository getRepository() {
        return repository;
    }

    public CacheAwareExternalResourceAccessor getResourceAccessor() {
        return resourceAccessor;
    }

    private static class NoOpCacheAwareExternalResourceAccessor implements CacheAwareExternalResourceAccessor {
        private final FileResourceConnector connector;

        public NoOpCacheAwareExternalResourceAccessor(FileResourceConnector connector) {
            this.connector = connector;
        }

        public LocallyAvailableExternalResource getResource(URI source, ResourceFileStore fileStore, @Nullable LocallyAvailableResourceCandidates localCandidates) throws IOException {
            return connector.getResource(source);
        }
    }
}
