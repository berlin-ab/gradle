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

package org.gradle.integtests.tooling.r112

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.eclipse.EclipseProject

class ToolingApiDeprecationsCrossVersionSpec extends ToolingApiSpecification {
    def setup() {
        file("build.gradle") << """
task noop << {
    println "noop"
}
"""
    }

    @ToolingApiVersion(">=1.12")
    @TargetGradleVersion("<1.0-milestone-8")
    def "build rejected for pre 1.0m8 providers"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardOutput = output
            build.forTasks("noop")
            build.run()
        }

        then:
        UnsupportedVersionException e = thrown()
        e != null
    }

    @ToolingApiVersion(">=1.12")
    @TargetGradleVersion("<1.0-milestone-8")
    def "model retrieving fails for pre 1.0m8 providers"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def modelBuilder = connection.model(EclipseProject)
            modelBuilder.standardOutput = output
            modelBuilder.get()
        }

        then:
        UnsupportedVersionException e = thrown()
        e != null
    }

    @ToolingApiVersion(">=1.12")
    @TargetGradleVersion(">=1.0-milestone-8")
    def "build shows no deprecation warning for 1.0m8+ providers"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardOutput = output
            build.forTasks("noop")
            build.run()
        }

        then:
        !output.toString().contains(deprecationMessageProvider(targetDist.version.version))
    }

    @ToolingApiVersion(">=1.12")
    @TargetGradleVersion(">=1.0-milestone-8")
    def "model retrieving shows no deprecation warning for 1.0m8+ providers"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def modelBuilder = connection.model(GradleProject)
            modelBuilder.standardOutput = output
            modelBuilder.get()
        }

        then:
        !output.toString().contains(deprecationMessageProvider(targetDist.version.version))
    }

    def deprecationMessageProvider(def version) {
        "Connecting to Gradle version " + version + " from the Gradle tooling API has been deprecated and is scheduled to be removed in version 2.0 of the Gradle tooling API"
    }

    @ToolingApiVersion("<1.2")
    @TargetGradleVersion(">=1.12")
    def "provider rejects build request from old toolingApi (pre 1.2)"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardOutput = output
            build.forTasks("noop")
            build.run()
        }

        then:
        GradleConnectionException e = thrown()
        e != null
    }

    @ToolingApiVersion(">=1.2")
    @TargetGradleVersion(">=1.12")
    def "provider shows no deprecation warning when build is requested by supported toolingApi"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def build = connection.newBuild()
            build.standardOutput = output
            build.forTasks("noop")
            build.run()
        }

        then:
        !output.toString().contains(deprecationMessageApi(targetDist.version.version))
    }

    @ToolingApiVersion("<1.2")
    @TargetGradleVersion(">=1.12")
    def "provider rejects model request from old toolingApi"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def modelBuilder = connection.model(EclipseProject)
            modelBuilder.standardOutput = output
            modelBuilder.get()
        }

        then:
        GradleConnectionException e = thrown()
        e != null
        // TODO cause is UnsupportedVersionException?
    }

    @ToolingApiVersion(">=1.2")
    @TargetGradleVersion(">=1.12")
    def "provider shows no deprecation warning when model is requested by supported toolingApi"() {
        when:
        def output = new ByteArrayOutputStream()
        withConnection { ProjectConnection connection ->
            def modelBuilder = connection.model(EclipseProject)
            modelBuilder.standardOutput = output
            modelBuilder.get()
        }

        then:
        !output.toString().contains(deprecationMessageApi(targetDist.version.version))
    }

    def deprecationMessageApi(def version) {
        "Connection from tooling API older than version 1.2 has been deprecated and is scheduled to be removed in Gradle"
    }

}
