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
package cloud.piranha.cdi.weld;

import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.weld.environment.servlet.Container;
import org.jboss.weld.environment.servlet.ContainerContext;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.resources.spi.ResourceLoader;

/**
 * The Weld container.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class WeldContainer implements Container {

    /**
     * Destroy the container.
     *
     * @param context the container context.
     */
    @Override
    public void destroy(ContainerContext context) {
    }

    /**
     * Initialize the container.
     *
     * @param context the container context.
     */
    @Override
    public void initialize(ContainerContext context) {
        try {
            CDI.setCDIProvider(new WeldProvider());
        } catch (IllegalStateException ise) {
        }
        try {
            WeldManager manager = context.getManager();
            WeldCDI cdi = new WeldCDI();
            cdi.setWeldManager(manager);
            WeldProvider.setCDI(cdi);
            InitialContext initialContext = new InitialContext();
            initialContext.rebind("java:comp/BeanManager", manager);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    /**
     * Touch the container.
     *
     * @param resourceLoader the resource loader.
     * @param context the container context.
     * @return true
     * @throws Exception when a serious error occurs.
     */
    @Override
    public boolean touch(ResourceLoader resourceLoader, ContainerContext context) throws Exception {
        return true;
    }
}