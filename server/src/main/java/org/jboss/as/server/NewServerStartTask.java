/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.server;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceActivator;

/**
 * This is the task used by the Host Controller and passed to a Server instance
 * in order to bootstrap it from a remote source process.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class NewServerStartTask implements ServerTask, Serializable, ObjectInputValidation {

    private static final long serialVersionUID = -8505496119636153918L;

    private final String serverName;
    private final int portOffset;
    private final List<ServiceActivator> startServices;
    private final List<ModelNode> updates;
    private final ServerEnvironment providedEnvironment;

    public NewServerStartTask(final String serverName, final int portOffset, final List<ServiceActivator> startServices, final List<ModelNode> updates) {
        if (serverName == null || serverName.length() == 0) {
            throw new IllegalArgumentException("Server name " + serverName + " is invalid; cannot be null or blank");
        }
        this.serverName = serverName;
        this.portOffset = portOffset;
        this.startServices = startServices;
        this.updates = updates;
        final Properties properties = System.getProperties();
        properties.setProperty("jboss.server.name", serverName);
        properties.setProperty("jboss.server.deploy.dir", properties.getProperty("jboss.home.dir") + "/domain/content");
        providedEnvironment = new ServerEnvironment(properties, System.getenv(), false);
    }

    public void run(final List<ServiceActivator> runServices) {
        final NewBootstrap bootstrap = NewBootstrap.Factory.newInstance();
        final NewBootstrap.Configuration configuration = new NewBootstrap.Configuration();
        configuration.setServerEnvironment(providedEnvironment);
        configuration.setConfigurationPersister(new NewConfigurationPersister() {
            public void store(final ModelNode model) throws ConfigurationPersistenceException {
            }

            public List<ModelNode> load() throws ConfigurationPersistenceException {
                return updates;
            }
        });
        configuration.setPortOffset(portOffset);
        bootstrap.start(configuration, startServices);
    }

    public void validateObject() throws InvalidObjectException {
        if (serverName == null) {
            throw new InvalidObjectException("serverName is null");
        }
        if (portOffset < 0) {
            throw new InvalidObjectException("portOffset is out of range");
        }
        if (updates == null) {
            throw new InvalidObjectException("updates is null");
        }
        if (startServices == null) {
            throw new InvalidObjectException("startServices is null");
        }
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        ois.registerValidation(this, 100);
    }
}
