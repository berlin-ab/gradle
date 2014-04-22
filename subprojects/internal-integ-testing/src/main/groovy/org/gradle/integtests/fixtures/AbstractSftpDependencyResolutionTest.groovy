/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.integtests.fixtures

import org.gradle.test.fixtures.ivy.IvySftpRepository
import org.gradle.test.fixtures.maven.MavenSftpRepository
import org.gradle.test.fixtures.server.sftp.SFTPServer
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Rule

@Requires(TestPrecondition.JDK6_OR_LATER)
class AbstractSftpDependencyResolutionTest extends AbstractDependencyResolutionTest {
    @Rule final SFTPServer server = new SFTPServer(this)

    MavenSftpRepository getMavenSftpRepo() {
        new MavenSftpRepository(server, '/repo')
    }

    IvySftpRepository getIvySftpRepo(boolean m2Compatible, String dirPattern = null) {
        new IvySftpRepository(server, '/repo', m2Compatible, dirPattern)
    }

    IvySftpRepository getIvySftpRepo(String contextPath) {
        new IvySftpRepository(server, contextPath, false, null)
    }

    IvySftpRepository getIvySftpRepo() {
        new IvySftpRepository(server, '/repo')
    }
}