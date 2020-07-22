/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its 
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.webapp.impl;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JUnit tests for testing everything related to the
 * ServletContainerInitializer API.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class ServletContainerInitializerTest {

    /**
     * Test addInitializer method.
     *
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testAddInitializer() throws Exception {
        DefaultWebApplication webApp = new DefaultWebApplication();
        webApp.addInitializer(new TestInitializer());
        webApp.initialize();
        assertNotNull(webApp.getAttribute("initializerCalled"));
    }

    /**
     * Test addInitializer method.
     *
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testAddInitializer2() throws Exception {
        DefaultWebApplication webApp = new DefaultWebApplication();
        webApp.addInitializer("this.does.not.exist");
        webApp.initialize();
    }

    /**
     * Test addInitializer method.
     *
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testAddInitializer3() throws Exception {
        DefaultWebApplication webApp = new DefaultWebApplication();
        webApp.addInitializer(TestInitializer.class.getName());
        webApp.initialize();
        assertNotNull(webApp.getAttribute("initializerCalled"));
    }

    @Test
    public void testInitializerWithHandlesTypes () {
        DefaultWebApplication webApp = new DefaultWebApplication();
        webApp.addInitializer(InitializerWithHandlesTypes.class.getName());
        webApp.initialize();
        assertTrue(webApp.getAttribute("object_class") instanceof Boolean);
        assertFalse((Boolean) webApp.getAttribute("object_class"));

        assertTrue(webApp.getAttribute("someannotation_class") instanceof Boolean);
        assertFalse((Boolean) webApp.getAttribute("someannotation_class"));
    }

    @Test
    public void testInitializerWithHandlesTypes2 () {
        DefaultWebApplication webApp = new DefaultWebApplication();
        webApp.addInitializer(InitializerWithHandlesTypes.class.getName());
        DefaultAnnotationManager annotationManager = (DefaultAnnotationManager) webApp.getAnnotationManager();
        annotationManager.addInstance(Set.class, Collections.emptySet().getClass());
        annotationManager.addAnnotation(
            new DefaultAnnotationManager.DefaultAnnotationInfo<>(
                ClassAnnotated.class.getAnnotation(SomeAnnotation.class),
                ClassAnnotated.class
            )
        );

        webApp.initialize();
        assertTrue(webApp.getAttribute("object_class") instanceof Boolean);
        assertTrue((Boolean) webApp.getAttribute("object_class"));

        assertTrue(webApp.getAttribute("someannotation_class") instanceof Boolean);
        assertTrue((Boolean) webApp.getAttribute("someannotation_class"));
    }

    /**
     * A test ServletContainerInitializer used to make sure they are called when
     * the web application initializes.
     */
    public static class TestInitializer implements ServletContainerInitializer {

        /**
         * Constructor.
         */
        public TestInitializer() {
        }

        /**
         * Handle on startup.
         *
         * @param classes the classes.
         * @param servletContext the servlet context.
         * @throws ServletException when a servlet error occurs.
         */
        @Override
        public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
            servletContext.setAttribute("initializerCalled", true);
        }
    }

    @HandlesTypes({Set.class, SomeAnnotation.class})
    public static class InitializerWithHandlesTypes implements ServletContainerInitializer{
        @Override
        public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
            Optional<Class<?>> classInstance = classes.stream().filter(Set.class::isAssignableFrom).findFirst();
            servletContext.setAttribute("object_class", classInstance.isPresent());

            Optional<Class<?>> classWithAnnotation = classes.stream().filter(x -> x.getAnnotation(SomeAnnotation.class) != null).findFirst();
            servletContext.setAttribute("someannotation_class", classWithAnnotation.isPresent());
        }
    }

    @SomeAnnotation
    public static class ClassAnnotated {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SomeAnnotation {
    }

}
