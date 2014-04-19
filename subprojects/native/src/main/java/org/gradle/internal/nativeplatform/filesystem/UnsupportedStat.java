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

package org.gradle.internal.nativeplatform.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnsupportedStat extends FallbackStat {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnsupportedStat.class);
    private final AtomicBoolean warned = new AtomicBoolean();

    @Override
    public int getUnixMode(File f) throws IOException {
        if (warned.compareAndSet(false, true)) {
            LOGGER.warn("Support for reading file permissions is only available on this platform using Java 7 or later.");
        }
        return super.getUnixMode(f);
    }
}