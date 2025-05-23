/*-
 * ========================LICENSE_START=================================
 * Commons
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.support.classpath.scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ScannerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScannerTest.class);

    private File targetDir;
    private File classesDir;
    private File testClassesDir;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        targetDir = new File("target");
        classesDir = new File(targetDir, "classes");
        testClassesDir = new File(targetDir, "test-classes");
    }

    @Test
    public void testScanClasspathGivenInstanceOfFilterHavingIncludeList() throws IOException {
        InstanceOfFilter filter = new InstanceOfFilter(ScannerFilter.class, null, new String[]{"org/smooks"});
        Scanner scanner = new Scanner(filter);

        long start = System.currentTimeMillis();
        scanner.scanClasspath(newUrlClassLoader(targetDir, classesDir, testClassesDir));
        LOGGER.debug("Took: " + (System.currentTimeMillis() - start));
        List<Class<?>> classes = filter.getClasses();

        LOGGER.debug(classes.toString());
        assertEquals(5, classes.size());
        assertTrue(classes.contains(InstanceOfFilter.class));
        assertTrue(classes.contains(IsAnnotationPresentFilter.class));
        assertTrue(classes.contains(AbstractScannerFilter.class));
        assertTrue(classes.contains(ScannerFilter.class));
    }

    @Test
    public void testScanClasspathGivenIsAnnotationPresentFilter() throws IOException {
        IsAnnotationPresentFilter filter = new IsAnnotationPresentFilter(TestAnnotation.class, null, new String[]{"org/smooks"});
        Scanner scanner = new Scanner(filter);

        long start = System.currentTimeMillis();
        scanner.scanClasspath(newUrlClassLoader(targetDir, classesDir, testClassesDir));
        LOGGER.debug("Took: " + (System.currentTimeMillis() - start));
        List<Class<?>> classes = filter.getClasses();

        LOGGER.debug(classes.toString());
        assertEquals(2, classes.size());
        assertTrue(classes.contains(AnnotatedClass1.class));
        assertTrue(classes.contains(AnnotatedClass1.class));
    }

    @Test
    public void testScanClasspathGivenInstanceOfFilter() throws IOException {
        InstanceOfFilter filter = new InstanceOfFilter(ScannerFilter.class);
        Scanner scanner = new Scanner(filter);

        long start = System.currentTimeMillis();
        scanner.scanClasspath(newUrlClassLoader(targetDir, classesDir, testClassesDir));
        LOGGER.debug("Took: " + (System.currentTimeMillis() - start));
        List<Class<?>> classes = filter.getClasses();

        LOGGER.debug(classes.toString());
        assertEquals(5, classes.size());
        assertTrue(classes.contains(InstanceOfFilter.class));
        assertTrue(classes.contains(IsAnnotationPresentFilter.class));
        assertTrue(classes.contains(AbstractScannerFilter.class));
        assertTrue(classes.contains(ScannerFilter.class));
    }

    @Test
    public void testScanClasspathSkipsFilesThatAreNotArchives() throws IOException {
        Scanner scanner = new Scanner(new ScannerFilter() {
            @Override
            public void filter(String resourceName) {

            }

            @Override
            public boolean isIgnorable(String resourceName) {
                return false;
            }
        });

        scanner.scanClasspath(newUrlClassLoader(targetDir, classesDir, testClassesDir, new File(testClassesDir, "jaxws-ri-4.0.1.pom")));
    }

    private URLClassLoader newUrlClassLoader(File... files) throws MalformedURLException {
        URL[] urls = new URL[files.length];
        int i = 0;
        for (File file : files) {
            urls[i] = file.toURI().toURL();
            i++;
        }

        return new URLClassLoader(urls);
    }
}
