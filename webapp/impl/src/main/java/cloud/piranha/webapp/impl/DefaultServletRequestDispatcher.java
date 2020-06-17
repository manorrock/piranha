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

import static cloud.piranha.webapp.api.CurrentRequestHolder.CURRENT_REQUEST_ATTRIBUTE;
import static java.util.Arrays.asList;
import static javax.servlet.AsyncContext.ASYNC_CONTEXT_PATH;
import static javax.servlet.AsyncContext.ASYNC_PATH_INFO;
import static javax.servlet.AsyncContext.ASYNC_QUERY_STRING;
import static javax.servlet.AsyncContext.ASYNC_REQUEST_URI;
import static javax.servlet.AsyncContext.ASYNC_SERVLET_PATH;
import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.FORWARD;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cloud.piranha.webapp.api.CurrentRequestHolder;
import cloud.piranha.webapp.utils.AsyncHttpDispatchWrapper;

/**
 * The default ServletRequestDispatcher.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class DefaultServletRequestDispatcher implements RequestDispatcher {

    /**
     * The servletEnvironment corresponding to the target resource to which this dispatcher forwards or includes.
     * 
     * <p>
     * It contains the actual Servlet, to process the forwarded or included request, as well as meta data for
     * this Servlet.
     */
    private final DefaultServletEnvironment servletEnvironment;
    /**
     * Stores the path.
     */
    private final String path;

    /**
     * Constructor.
     *
     * @param servletEnvironment the servlet environment.
     * @param path the path.
     */
    public DefaultServletRequestDispatcher(DefaultServletEnvironment servletEnvironment, String path) {
        this.servletEnvironment = servletEnvironment;
        this.path = path;
    }

    /**
     * Forward the request and response.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws ServletException when a servlet error occurs.
     * @throws IOException when an I/O error occurs.
     */
    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        if (servletRequest.getDispatcherType().equals(ASYNC)) {
                
                if (servletRequest instanceof AsyncHttpDispatchWrapper == false) {
                    throw new IllegalStateException("Async invocations without AsyncHttpDispatchWrapper not supported at this moment.");
                }
                
                AsyncHttpDispatchWrapper asyncHttpDispatchWrapper = (AsyncHttpDispatchWrapper) servletRequest;
                
                
                HttpServletRequest asyncStartRequest = asyncHttpDispatchWrapper.getRequest();
                
                if (asyncStartRequest instanceof DefaultWebApplicationRequest) {
                    
                }

                if (path != null) {
                    
                    asList(ASYNC_CONTEXT_PATH, ASYNC_PATH_INFO, ASYNC_QUERY_STRING, ASYNC_REQUEST_URI, ASYNC_SERVLET_PATH).forEach(attribute -> {
                        // Set the spec demanded attributes on asyncHttpDispatchWrapper with the values taken from asyncStartRequest
                        setForwardAttribute(asyncStartRequest, asyncHttpDispatchWrapper, attribute);
                        
                        // Record that this attribute lives in the wrapper
                        asyncHttpDispatchWrapper.getWrapperAttributes().add(attribute);
                    });
                    

                    asyncHttpDispatchWrapper.setServletPath(getServletPath(path));
                    
                    // TODO: this is likely not entirely correct, maybe needs to be done earlier
                    String queryString = getQueryString(path);
                    if (queryString != null && !queryString.isEmpty()) {
                        asyncHttpDispatchWrapper.setQueryString(queryString);
                    } else {
                        asyncHttpDispatchWrapper.setQueryString(asyncStartRequest.getQueryString());
                    }
                    
                    asyncHttpDispatchWrapper.setAttribute("PREVIOUS_REQUEST", asyncStartRequest);
                    asyncHttpDispatchWrapper.getWrapperAttributes().add("PREVIOUS_REQUEST");

                } else {
                    asyncHttpDispatchWrapper.setServletPath("/" + servletEnvironment.getServletName());
                }
                

                servletEnvironment.getWebApplication().linkRequestAndResponse(asyncHttpDispatchWrapper, servletResponse);
                servletEnvironment.getServlet().service(asyncHttpDispatchWrapper, servletResponse);
                servletEnvironment.getWebApplication().unlinkRequestAndResponse(asyncHttpDispatchWrapper, servletResponse);
                
                
                servletResponse.flushBuffer();
        } else {
            try (DefaultWebApplicationRequest forwardedRequest = new DefaultWebApplicationRequest()) {
                
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                
                response.resetBuffer();
                
                forwardedRequest.setWebApplication(servletEnvironment.getWebApplication());
                forwardedRequest.setContextPath(request.getContextPath());
                forwardedRequest.setDispatcherType(FORWARD);
                forwardedRequest.setAsyncSupported(request.isAsyncSupported());

                if (path != null) {
                    setForwardAttributes(request, forwardedRequest, 
                        FORWARD_CONTEXT_PATH, 
                        FORWARD_PATH_INFO, 
                        FORWARD_QUERY_STRING,
                        FORWARD_REQUEST_URI, 
                        FORWARD_SERVLET_PATH);

                    forwardedRequest.setServletPath(getServletPath(path));
                    forwardedRequest.setQueryString(getQueryString(path));

                } else {
                    forwardedRequest.setServletPath("/" + servletEnvironment.getServletName());
                }
                
                CurrentRequestHolder currentRequestHolder = updateCurrentRequest(request, forwardedRequest);

                try {
                    servletEnvironment.getWebApplication().linkRequestAndResponse(forwardedRequest, servletResponse);
                    servletEnvironment.getServlet().service(forwardedRequest, servletResponse);
                    servletEnvironment.getWebApplication().unlinkRequestAndResponse(forwardedRequest, servletResponse);
                } finally {
                    restoreCurrentRequest(currentRequestHolder, request);
                }
                
                response.flushBuffer();
            }
        }
    }

    /**
     * Include the request and response.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws ServletException when a servlet error occurs.
     * @throws IOException when an I/O error occurs.
     */
    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        try (DefaultWebApplicationRequest includedRequest = new DefaultWebApplicationRequest()) {
            HttpServletRequest originalRequest = (HttpServletRequest) servletRequest;
            includedRequest.setWebApplication(servletEnvironment.getWebApplication());
            includedRequest.setContextPath(originalRequest.getContextPath());
            includedRequest.setAttribute(INCLUDE_REQUEST_URI, originalRequest.getRequestURI());
            includedRequest.setAttribute(INCLUDE_CONTEXT_PATH, originalRequest.getContextPath());
            includedRequest.setAttribute(INCLUDE_SERVLET_PATH, originalRequest.getServletPath());
            includedRequest.setAttribute(INCLUDE_PATH_INFO, originalRequest.getPathInfo());
            includedRequest.setAttribute(INCLUDE_QUERY_STRING, originalRequest.getQueryString());
            includedRequest.setServletPath(path);
            includedRequest.setPathInfo(null);
            includedRequest.setQueryString(null);
            
            servletEnvironment.getServlet().service(servletRequest, servletResponse);
        }
    }
    
    private void setForwardAttributes(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest, String... dispatcherKeys) {
        for (String dispatcherKey : dispatcherKeys) {
            setForwardAttribute(originalRequest, forwardedRequest, dispatcherKey);
        }
        
    }
    
    /**
     * Set forward attribute.
     *
     * @param originalRequest the original request
     * @param forwardedRequest the forward request.
     * @param dispatcherKey the dispatcher key.
     */
    private void setForwardAttribute(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest, String dispatcherKey) {
        String value = null;

        if (originalRequest.getAttribute(dispatcherKey) != null) {
            value = (String) originalRequest.getAttribute(dispatcherKey);
        } else {
            if (dispatcherKey.equals(FORWARD_CONTEXT_PATH)) {
                value = originalRequest.getContextPath();
            }
            if (dispatcherKey.equals(FORWARD_PATH_INFO)) {
                value = originalRequest.getPathInfo();
            }
            if (dispatcherKey.equals(FORWARD_QUERY_STRING)) {
                value = originalRequest.getQueryString();
            }
            if (dispatcherKey.equals(FORWARD_REQUEST_URI)) {
                value = originalRequest.getRequestURI();
            }
            if (dispatcherKey.equals(FORWARD_SERVLET_PATH)) {
                value = originalRequest.getServletPath();
            }
        }

        forwardedRequest.setAttribute(dispatcherKey, value);
    }
    
    private CurrentRequestHolder updateCurrentRequest(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest) {
        CurrentRequestHolder currentRequestHolder = (CurrentRequestHolder) originalRequest.getAttribute(CURRENT_REQUEST_ATTRIBUTE);
        if (currentRequestHolder != null) {
            currentRequestHolder.setRequest(forwardedRequest);
            forwardedRequest.setAttribute(CURRENT_REQUEST_ATTRIBUTE, currentRequestHolder);
        }
        
        forwardedRequest.setAttribute("PREVIOUS_REQUEST", originalRequest);
        
        return currentRequestHolder;
    }
    
    private void restoreCurrentRequest(CurrentRequestHolder currentRequestHolder, HttpServletRequest originalRequest) {
        if (currentRequestHolder != null) {
            currentRequestHolder.setRequest(originalRequest);
        }
    }
    
    private String getServletPath(String path) {
        return !path.contains("?") ? path : path.substring(0, path.indexOf("?"));
    }
    
    private String getQueryString(String path) {
        return !path.contains("?") ? null : path.substring(path.indexOf("?") + 1);
    }
}
