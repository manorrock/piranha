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
package cloud.piranha.webapp.webxml;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.stream.Collectors.toSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;

import cloud.piranha.webapp.api.WebApplication;
import cloud.piranha.webapp.api.WelcomeFileManager;
import cloud.piranha.webapp.impl.WebXml;
import cloud.piranha.webapp.impl.WebXmlContextParam;
import cloud.piranha.webapp.impl.WebXmlErrorPage;
import cloud.piranha.webapp.impl.WebXmlFilterInitParam;
import cloud.piranha.webapp.impl.WebXmlListener;
import cloud.piranha.webapp.impl.WebXmlMimeMapping;
import cloud.piranha.webapp.impl.WebXmlServlet;
import cloud.piranha.webapp.impl.WebXmlServletMapping;

/**
 * The web.xml / web-fragment.xml processor.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class WebXmlProcessor {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(WebXmlProcessor.class.getName());

    private static final String[] STRING_ARRAY = new String[0];

    /**
     * Process the web.xml into the web application.
     *
     * @param webXml the web.xml
     * @param webApplication the web application.
     */
    public void process(WebXml webXml, WebApplication webApplication) {
        LOGGER.log(FINER, "Started WebXmlProcessor.process");
        processContextParameters(webApplication, webXml);
        processDefaultContextPath(webApplication, webXml);
        processDenyUncoveredHttpMethods(webApplication, webXml);
        processDisplayName(webApplication, webXml);
        processDistributable(webApplication, webXml);
        processErrorPages(webApplication, webXml);
        processFilters(webApplication, webXml);
        processFilterMappings(webApplication, webXml);
        processListeners(webApplication, webXml);
        processMimeMappings(webApplication, webXml);
        processRequestCharacterEncoding(webApplication, webXml);
        processResponseCharacterEncoding(webApplication, webXml);
        processRoleNames(webApplication, webXml);
        processServlets(webApplication, webXml);
        processServletMappings(webApplication, webXml);
        processWebApp(webApplication, webXml);
        processWelcomeFiles(webApplication, webXml);
        LOGGER.log(FINER, "Finished WebXmlProcessor.process");
    }

    /**
     * Process the context parameters.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processContextParameters(WebApplication webApplication, WebXml webXml) {
        Iterator<WebXmlContextParam> iterator = webXml.getContextParams().iterator();
        while (iterator.hasNext()) {
            WebXmlContextParam contextParam = iterator.next();
            webApplication.setInitParameter(contextParam.getName(), contextParam.getValue());
        }
    }

    /**
     * Process the default context path.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDefaultContextPath(WebApplication webApplication, WebXml webXml) {
        if (webXml.getDefaultContextPath() != null) {
            webApplication.setContextPath(webXml.getDefaultContextPath());
        }
    }

    /**
     * Process the deny uncovered HTTP methods flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDenyUncoveredHttpMethods(WebApplication webApplication, WebXml webXml) {
        webApplication.setDenyUncoveredHttpMethods(webXml.getDenyUncoveredHttpMethods());
    }

    /**
     * Process the display name flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDisplayName(WebApplication webApplication, WebXml webXml) {
        webApplication.setServletContextName(webXml.getDisplayName());
    }

    /**
     * Process the distributable flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDistributable(WebApplication webApplication, WebXml webXml) {
        webApplication.setDistributable(webXml.isDistributable());
    }

    /**
     * Process the error pages.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processErrorPages(WebApplication webApplication, WebXml webXml) {
        Iterator<WebXmlErrorPage> iterator = webXml.getErrorPages().iterator();
        while (iterator.hasNext()) {
            WebXmlErrorPage errorPage = iterator.next();
            if (errorPage.getErrorCode() != null && !errorPage.getErrorCode().isEmpty()) {
                webApplication.addErrorPage(Integer.parseInt(errorPage.getErrorCode()), errorPage.getLocation());
            } else if (errorPage.getExceptionType() != null && !errorPage.getExceptionType().isEmpty()) {
                webApplication.addErrorPage(errorPage.getExceptionType(), errorPage.getLocation());
            }
        }
    }

    /**
     * Process the filter mappings mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processFilterMappings(WebApplication webApplication, WebXml webXml) {
        webXml.getFilterMappings().forEach(filterMapping -> {
            // Filter is mapped to a URL pattern, e.g. /path/customer
            webApplication.addFilterMapping(
                toDispatcherTypes(filterMapping.getDispatchers()),
                filterMapping.getFilterName(),
                true,
                filterMapping.getUrlPatterns().toArray(STRING_ARRAY));

            // Filter is mapped to a named Servlet, e.g. FacesServlet
            webApplication.addFilterMapping(
                toDispatcherTypes(filterMapping.getDispatchers()),
                filterMapping.getFilterName(),
                true,
                filterMapping.getServletNames().stream().map(e -> "servlet:// " + e).toArray(String[]::new));
        });
    }

    private Set<DispatcherType> toDispatcherTypes(List<String> dispatchers) {
        if (dispatchers == null) {
            return null;
        }

        return
            dispatchers.stream()
                       .map(e -> DispatcherType.valueOf(e))
                       .collect(toSet());
    }

    /**
     * Process the filters.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processFilters(WebApplication webApplication, WebXml webXml) {
        webXml.getFilters().forEach((filter) -> {
            FilterRegistration.Dynamic dynamic = null;

            if (filter.getClassName() != null) {
                dynamic = webApplication.addFilter(filter.getFilterName(), filter.getClassName());
            } else if (filter.getServletName() != null) {
                dynamic = webApplication.addFilter(filter.getFilterName(), filter.getServletName());
            }

            if (dynamic != null) {
                for (WebXmlFilterInitParam initParam : filter.getInitParams()) {
                    dynamic.setInitParameter(initParam.getName(), initParam.getValue());
                }
            }
        });
    }

    /**
     * Process the listeners.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processListeners(WebApplication webApplication, WebXml webXml) {
        Iterator<WebXmlListener> iterator = webXml.getListeners().iterator();
        while (iterator.hasNext()) {
            WebXmlListener listener = iterator.next();
            webApplication.addListener(listener.getClassName());
        }
    }

    /**
     * Process the mime mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processMimeMappings(WebApplication webApplication, WebXml webXml) {
        Iterator<WebXmlMimeMapping> mappingIterator = webXml.getMimeMappings().iterator();
        while (mappingIterator.hasNext()) {
            WebXmlMimeMapping mapping = mappingIterator.next();
            webApplication.getMimeTypeManager()
                    .addMimeType(mapping.getExtension(), mapping.getMimeType());
        }
    }

    /**
     * Process the request character encoding.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processRequestCharacterEncoding(WebApplication webApplication, WebXml webXml) {
        if (webXml.getRequestCharacterEncoding() != null) {
            webApplication.setRequestCharacterEncoding(webXml.getRequestCharacterEncoding());
        }
    }

    /**
     * Process the response character encoding.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processResponseCharacterEncoding(WebApplication webApplication, WebXml webXml) {
        if (webXml.getResponseCharacterEncoding() != null) {
            webApplication.setResponseCharacterEncoding(webXml.getResponseCharacterEncoding());
        }
    }

    private void processRoleNames(WebApplication webApplication, WebXml webXml) {
        webApplication.getSecurityManager().declareRoles(webXml.getRoleNames());
    }

    /**
     * Process the servlet mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processServletMappings(WebApplication webApplication, WebXml webXml) {
        Iterator<WebXmlServletMapping> iterator = webXml.getServletMappings().iterator();
        while (iterator.hasNext()) {
            WebXmlServletMapping mapping = iterator.next();
            webApplication.addServletMapping(
                    mapping.getServletName(), mapping.getUrlPattern());
        }
    }

    /**
     * Process the web app. This is basically only the version contained within it.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processWebApp(WebApplication webApplication, WebXml webXml) {
        webApplication.setEffectiveMajorVersion(webXml.getMajorVersion());
        webApplication.setEffectiveMinorVersion(webXml.getMinorVersion());
    }

    /**
     * Process the servlets.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processServlets(WebApplication webApplication, WebXml webXml) {
        LOGGER.log(FINE, "Configuring Servlets");

        Iterator<WebXmlServlet> iterator = webXml.getServlets().iterator();
        while (iterator.hasNext()) {
            WebXmlServlet servlet = iterator.next();
            LOGGER.log(FINE, () -> "Configuring Servlet: " + servlet.getServletName());

            ServletRegistration.Dynamic dynamic = webApplication.addServlet(servlet.getServletName(), servlet.getClassName());

            String jspFile = servlet.getJspFile();
            if (!isEmpty(jspFile))
                webApplication.addJspFile(servlet.getServletName(), jspFile);

            if (servlet.isAsyncSupported()) {
                dynamic.setAsyncSupported(true);
            }

            servlet.getInitParams().forEach(initParam -> {
                dynamic.setInitParameter(initParam.getName(), initParam.getValue());
            });

            LOGGER.log(FINE, () -> "Configured Servlet: " + servlet.getServletName());
        }
    }

    /**
     * Process the welcome files.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processWelcomeFiles(WebApplication webApplication, WebXml webXml) {
        LOGGER.log(FINE, "Adding welcome files");

        Iterator<String> iterator = webXml.getWelcomeFiles().iterator();
        WelcomeFileManager welcomeFileManager = webApplication.getWelcomeFileManager();
        while (iterator.hasNext()) {
            String welcomeFile = iterator.next();
            LOGGER.log(FINE, () -> "Adding welcome file: " + welcomeFile);
            welcomeFileManager.addWelcomeFile(welcomeFile);
        }
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
