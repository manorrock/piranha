/*
 * Copyright (c) 2002-2021 Manorrock.com. All Rights Reserved.
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
package cloud.piranha.embedded;

import cloud.piranha.api.Piranha;
import cloud.piranha.naming.thread.ThreadInitialContextFactory;
import cloud.piranha.resource.ByteArrayResourceStreamHandlerProvider;
import cloud.piranha.webapp.impl.DefaultWebApplication;
import cloud.piranha.webapp.api.WebApplication;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * The embeddable servlet container version of Piranha.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class EmbeddedPiranha implements Piranha {

    /**
     * Stores the web application.
     */
    private final DefaultWebApplication webApplication;

    /**
     * Constructor.
     */
    public EmbeddedPiranha() {
        webApplication = new DefaultWebApplication();
    }

    /**
     * Destroy the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha destroy() {
        webApplication.destroy();
        return this;
    }

    /**
     * Get the version.
     *
     * @return the version.
     */
    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    /**
     * Get the web application.
     *
     * @return the web application.
     */
    public WebApplication getWebApplication() {
        return webApplication;
    }

    /**
     * Initialize the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha initialize() {
        webApplication.initialize();
        return this;
    }

    /**
     * Service method.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws IOException when an I/O error occurs.
     * @throws ServletException when a Servlet error occurs.
     */
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        try {
            ThreadInitialContextFactory.setInitialContext(webApplication.getNamingManager().getContext());
            ByteArrayResourceStreamHandlerProvider.setGetResourceAsStreamFunction(e -> webApplication.getResourceAsStream(e));

            if (servletRequest.getServletContext() == null && servletRequest instanceof EmbeddedRequest) {
                EmbeddedRequest embeddedRequest = (EmbeddedRequest) servletRequest;
                embeddedRequest.setWebApplication(webApplication);
            }
            if (servletResponse instanceof EmbeddedResponse) {
                EmbeddedResponse embeddedResponse = (EmbeddedResponse) servletResponse;
                embeddedResponse.setWebApplication(webApplication);
            }
            
            webApplication.linkRequestAndResponse(servletRequest, servletResponse);
            webApplication.service(servletRequest, servletResponse);
            webApplication.unlinkRequestAndResponse(servletRequest, servletResponse);

        } finally {
            ThreadInitialContextFactory.removeInitialContext();
            ByteArrayResourceStreamHandlerProvider.setGetResourceAsStreamFunction(null);
        }
    }

    /**
     * Start the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha start() {
        webApplication.start();
        return this;
    }

    /**
     * Stop the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha stop() {
        webApplication.stop();
        return this;
    }
}
