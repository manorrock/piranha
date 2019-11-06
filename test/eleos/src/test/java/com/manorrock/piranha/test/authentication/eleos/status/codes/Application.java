/*
 * Copyright (c) 2002-2019 Manorrock.com. All Rights Reserved.
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
package com.manorrock.piranha.test.authentication.eleos.status.codes;

import static com.manorrock.piranha.authentication.elios.AuthenticationInitializer.AUTH_MODULE_CLASS;
import static com.manorrock.piranha.authorization.exousia.AuthorizationPreInitializer.AUTHZ_FACTORY_CLASS;
import static com.manorrock.piranha.authorization.exousia.AuthorizationPreInitializer.AUTHZ_POLICY_CLASS;
import static com.manorrock.piranha.authorization.exousia.AuthorizationPreInitializer.CONSTRAINTS;
import static com.manorrock.piranha.builder.WebApplicationBuilder.newWebApplication;
import static java.util.Arrays.asList;

import org.omnifaces.exousia.constraints.SecurityConstraint;
import org.omnifaces.exousia.modules.def.DefaultPolicy;
import org.omnifaces.exousia.modules.def.DefaultPolicyConfigurationFactory;

import com.manorrock.piranha.authentication.elios.AuthenticationInitializer;
import com.manorrock.piranha.authorization.exousia.AuthorizationInitializer;
import com.manorrock.piranha.authorization.exousia.AuthorizationPreInitializer;
import com.manorrock.piranha.security.jakarta.JakartaSecurityInitializer;
import com.manorrock.piranha.test.utils.TestWebApp;

/**
 * @author Arjan Tijms
 */
public class Application {
    
    public static TestWebApp get() {
        return 
            new TestWebApp(newWebApplication()
                .addAttribute(AUTHZ_FACTORY_CLASS, DefaultPolicyConfigurationFactory.class)
                .addAttribute(AUTHZ_POLICY_CLASS, DefaultPolicy.class)
                .addAttribute(CONSTRAINTS, asList(
                    new SecurityConstraint("/protected/servlet", "architect")))
                
                .addInitializer(AuthorizationPreInitializer.class)
            
                .addAttribute(AUTH_MODULE_CLASS, TestServerAuthModule.class)
                .addInitializer(AuthenticationInitializer.class)
                
                .addInitializer(AuthorizationInitializer.class)
                .addInitializer(JakartaSecurityInitializer.class)
                
                .addServlet(PublicServlet.class, "/public/servlet")
                .addServlet(ProtectedServlet.class, "/protected/servlet")
                .start());
    }

}
