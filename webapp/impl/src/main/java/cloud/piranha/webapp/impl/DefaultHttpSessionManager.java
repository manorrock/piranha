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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import cloud.piranha.webapp.api.HttpSessionManager;
import cloud.piranha.webapp.api.WebApplication;

/**
 * The default HttpSessionManager.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class DefaultHttpSessionManager implements HttpSessionManager, SessionCookieConfig {

    /**
     * Stores the session listeners.
     */
    protected final ArrayList<HttpSessionAttributeListener> attributeListeners;

    /**
     * Stores the comment.
     */
    protected String comment;

    /**
     * Stores the default session tracking modes.
     */
    protected final Set<SessionTrackingMode> defaultSessionTrackingModes;

    /**
     * Stores the domain.
     */
    protected String domain;

    /**
     * Stores the HTTP only flag.
     */
    protected boolean httpOnly;

    /**
     * Stores the session id listeners.
     */
    protected final ArrayList<HttpSessionIdListener> idListeners;

    /**
     * Stores the max age.
     */
    protected int maxAge;

    /**
     * Stores the name.
     */
    protected String name;

    /**
     * Stores the path.
     */
    protected String path;

    /**
     * Stores the secure flag.
     */
    protected boolean secure;

    /**
     * Stores the session listeners.
     */
    protected final ArrayList<HttpSessionListener> sessionListeners;

    /**
     * Stores the session timeout (in minutes).
     */
    protected int sessionTimeout;

    /**
     * Stores the session tracking modes.
     */
    protected Set<SessionTrackingMode> sessionTrackingModes;

    /**
     * Stores the sessions.
     */
    protected Map<String, HttpSession> sessions;

    /**
     * Stores the web application.
     */
    protected WebApplication webApplication;

    /**
     * Constructor.
     */
    public DefaultHttpSessionManager() {
        attributeListeners = new ArrayList<>(1);
        defaultSessionTrackingModes = EnumSet.of(SessionTrackingMode.COOKIE);
        sessionTrackingModes = defaultSessionTrackingModes;
        idListeners = new ArrayList<>(1);
        name = "JSESSIONID";
        sessionListeners = new ArrayList<>(1);
        sessionTimeout = 10;
        sessions = new ConcurrentHashMap<>();
    }

    /**
     * Create the session.
     *
     * @param webApplication the web application.
     * @param request the request.
     * @return the session.
     */
    @Override
    public synchronized HttpSession createSession(WebApplication webApplication, HttpServletRequest request) {
        String sessionId = UUID.randomUUID().toString();
        DefaultHttpSession session = new DefaultHttpSession(webApplication, sessionId, true);
        session.setSessionManager(this);
        sessions.put(sessionId, session);

        HttpServletResponse response = (HttpServletResponse) webApplication.getResponse(request);
        Cookie cookie = new Cookie(name, sessionId);

        if (path != null) {
            cookie.setPath(path);
        } else {
            cookie.setPath("".equals(webApplication.getContextPath())? "/" : webApplication.getContextPath());
        }

        response.addCookie(cookie);

        sessionListeners.stream().forEach((sessionListener) -> {
            sessionListener.sessionCreated(new HttpSessionEvent(session));
        });

        return session;
    }

    /**
     * Get the session.
     *
     * @param webApplication the web application.
     * @param request the request.
     * @param currentSessionId the current session id.
     * @return the session.
     */
    @Override
    public HttpSession getSession(WebApplication webApplication, HttpServletRequest request, String currentSessionId) {
        return sessions.get(currentSessionId);
    }

    /**
     * Change the session id.
     *
     * @param request the request.
     * @return the session id.
     */
    @Override
    public String changeSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("No session active");
        }

        String oldSessionId = session.getId();
        sessions.remove(oldSessionId);
        String sessionId = UUID.randomUUID().toString();
        DefaultHttpSession newSession = (DefaultHttpSession) session;
        newSession.setId(sessionId);
        sessions.put(sessionId, session);

        idListeners.stream().forEach((idListener) -> {
            idListener.sessionIdChanged(new HttpSessionEvent(session), oldSessionId);
        });

        return sessionId;
    }

    /**
     * Add a listener.
     *
     * @param <T> the type.
     * @param listener the listener.
     */
    @Override
    public <T extends EventListener> void addListener(T listener) {
        if (listener instanceof HttpSessionAttributeListener) {
            attributeListeners.add((HttpSessionAttributeListener) listener);
        }

        if (listener instanceof HttpSessionIdListener) {
            idListeners.add((HttpSessionIdListener) listener);
        }

        if (listener instanceof HttpSessionListener) {
            sessionListeners.add((HttpSessionListener) listener);
        }
    }

    /**
     * Attribute added.
     *
     * @param session the HTTP session.
     * @param name the name.
     * @param value the value.
     */
    @Override
    public void attributeAdded(HttpSession session, String name, Object value) {
        attributeListeners.stream().forEach((listener) -> {
            listener.attributeAdded(new HttpSessionBindingEvent(session, name, value));
        });
    }

    /**
     * Attribute removed.
     *
     * @param session the HTTP session.
     * @param name the name.
     * @param value the value.
     */
    @Override
    public void attributeReplaced(HttpSession session, String name, Object value) {
        attributeListeners.stream().forEach((listener) -> {
            listener.attributeReplaced(new HttpSessionBindingEvent(session, name, value));
        });
    }

    /**
     * Attributed removed.
     *
     * @param session the HTTP session.
     * @param name the name.
     */
    @Override
    public void attributeRemoved(HttpSession session, String name) {
        attributeListeners.stream().forEach((listener) -> {
            listener.attributeRemoved(new HttpSessionBindingEvent(session, name));
        });
    }

    /**
     * Destroy the session.
     *
     * @param session the session.
     */
    @Override
    public synchronized void destroySession(HttpSession session) {
        sessionListeners.stream().forEach((sessionListener) -> {
            sessionListener.sessionDestroyed(new HttpSessionEvent(session));
        });
        sessions.remove(session.getId());
    }

    /**
     * Encode the redirect URL.
     *
     * @param response the response.
     * @param url the redirect url.
     * @return the encoded redirect url.
     */
    @Override
    public String encodeRedirectURL(HttpServletResponse response, String url) {
        return url;
    }

    /**
     * Encode the URL.
     *
     * @param response the response.
     * @param url the url.
     * @return the encoded url.
     */
    @Override
    public String encodeURL(HttpServletResponse response, String url) {
        return url;
    }

    /**
     * Get the comment.
     *
     * @return the comment.
     */
    @Override
    public String getComment() {
        return comment;
    }

    /**
     * Get the default session tracking modes.
     *
     * @return the default session tracking modes.
     */
    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Collections.unmodifiableSet(defaultSessionTrackingModes);
    }

    /**
     * Get the domain.
     *
     * @return the domain.
     */
    @Override
    public String getDomain() {
        return domain;
    }

    /**
     * Get the effective session tracking modes.
     *
     * @return the effective session tracking modes.
     */
    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.unmodifiableSet(sessionTrackingModes);
    }

    /**
     * Get the max age.
     *
     * @return the max age.
     */
    @Override
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * Get the name.
     *
     * @return the name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the path.
     *
     * @return the path.
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * Get the session cookie config.
     *
     * @return the session cookie config.
     */
    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return this;
    }

    /**
     * Get the session timeout (in minutes).
     *
     * @return the session timeout.
     */
    @Override
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Has a session with the given id.
     *
     * @param sessionId the session id.
     * @return true if there is one, false otherwise.
     */
    @Override
    public boolean hasSession(String sessionId) {
        return sessionId != null ? sessions.containsKey(sessionId) : false;
    }

    /**
     * Is HTTP only?
     *
     * @return true if HTTP only, false otherwise.
     */
    @Override
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Is secure.
     *
     * @return the secure flag.
     */
    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * Set the comment.
     *
     * @param comment the comment.
     */
    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Set the domain.
     *
     * @param domain the domain
     */
    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Set the HTTP only flag.
     *
     * @param httpOnly the HTTP only flag.
     */
    @Override
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Set the max age.
     *
     * @param maxAge the max age.
     */
    @Override
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Set the name.
     *
     * @param name the name.
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the path.
     *
     * @param path the path.
     */
    @Override
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Set the secure flag.
     *
     * @param secure the secure flag.
     */
    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * Set the session timeout.
     *
     * @param sessionTimeout the session timeout.
     */
    @Override
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * Set the session tracking modes.
     *
     * @param sessionTrackingModes the session tracking modes.
     */
    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        if (sessionTrackingModes.size() > 1 && sessionTrackingModes.contains(SessionTrackingMode.SSL)) {
            throw new IllegalArgumentException("SSL cannot be combined with any other method");
        }
        this.sessionTrackingModes = Collections.unmodifiableSet(sessionTrackingModes);
    }
}
