/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence;

import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.util.ApplicationContextManager;

public class PropagationManager {

    private static final Logger log =
            LoggerFactory.getLogger(PropagationManager.class);

    /**
     * Performs provisioning on each resource associated to the user.
     * Exceptions will be ignored.
     * @param user to be created.
     * @return a set of provisioned resources.
     */
    public Set<String> provision(SyncopeUser user) {
        return provision(user, false, false);
    }

    /**
     * Performs provisioning on each resource associated to the user.
     * If we ask for a synchronous provisioning passing true as second argument,
     * than exceptions won't be ignored but the process will be stoppend and a
     * runtime exception will be returned.
     * @param user to be created.
     * @param synchronous to ask for a synchronous or asynchronous provisioning.
     * @return a set of provisioned resources.
     */
    public Set<String> provision(SyncopeUser user, boolean synchronous) {
        return provision(user, synchronous, false);
    }

    /**
     * Performs update on each resource associated to the user.
     * Exceptions will be ignored.
     * @param user to be updated.
     * @return a set of updated resources.
     */
    public Set<String> update(SyncopeUser user) {
        return provision(user, false, true);
    }

    /**
     * Performs update on each resource associated to the user.
     * If we ask for a synchronous update passing true as second argument,
     * than exceptions won't be ignored but the process will be stoppend and a
     * runtime exception will be returned.
     * @param user to be updated.
     * @param synchronous to ask for a synchronous or asynchronous update.
     * @return a set of updated resources.
     */
    public Set<String> update(SyncopeUser user, boolean synchronous) {
        return provision(user, synchronous, true);
    }

    /**
     * Implementation of the provisioning feature.
     * @param user
     * @param synchronous
     * @param merge
     * @return
     */
    private Set<String> provision(
            SyncopeUser user, boolean synchronous, boolean merge) {

        Set<String> provisioned = new HashSet<String>();

        Set<Resource> resources = user.getResources();
        Set<SyncopeRole> roles = user.getRoles();

        for (SyncopeRole role : roles) {
            resources.addAll(role.getResources());
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Provisioning " + resources +
                    " with user " + user.getId());
        }

        for (Resource resource : resources) {
            try {

                propagate(user, resource, merge);
                provisioned.add(resource.getName());

            } catch (RuntimeException re) {

                if (log.isErrorEnabled()) {
                    log.error(
                            "Runtime exception during provision on resource " +
                            resource.getName(), re);
                }

                if (synchronous) {
                    throw re;
                }

            } catch (Throwable t) {

                if (log.isErrorEnabled()) {
                    log.error(
                            "Unknown exception during provision on resource " +
                            resource.getName(), t);
                }

                if (synchronous) {
                    throw new RuntimeException(t.getMessage());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Provisioned " + provisioned +
                    " with user " + user.getId());
        }

        return provisioned;
    }

    /**
     * Propagate provision/update the resource indicated.
     * @param user to be created.
     * @param resource to be provisioned.
     * @param merge specifies if it must be performed an update (true) or a
     * creation (false).
     * @throws NoSuchBeanDefinitionException if the connector bean doesn't
     * exist.
     * @throws IllegalStateException if propagation fails.
     */
    private void propagate(SyncopeUser user, Resource resource, boolean merge)
            throws NoSuchBeanDefinitionException, IllegalStateException {

        ConnectorInstance connectorInstance = resource.getConnector();

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) context.getBeanFactory();

        ConnectorFacade connector = (ConnectorFacade) beanFactory.getSingleton(
                connectorInstance.getId().toString());

        if (connector == null) {
            if (log.isErrorEnabled()) {

                log.error("Connector instance bean " +
                        connectorInstance.getId().toString() +
                        " not found");

            }

            throw new NoSuchBeanDefinitionException(
                    "Connector instance bean not found");
        }

        Set<SchemaMapping> mappings = resource.getMappings();

        Set<Attribute> attrs = new HashSet<Attribute>();

        String accountId = null;
        String field = null;
        String password = user.getPassword();

        for (SchemaMapping mapping : mappings) {

            field = mapping.getField();

            Object value = user.getAttribute(mapping.getUserSchema().getName());

            if (value != null && mapping.isAccountid()) {
                accountId = value.toString();
                attrs.add(new Name(accountId));
            }

            if (password != null && mapping.isPassword()) {
                attrs.add(AttributeBuilder.buildPassword(
                        password.toCharArray()));
            }

            if (!mapping.isPassword() && !mapping.isAccountid()) {
                attrs.add(AttributeBuilder.build(field, value));
            }
        }

        Uid userUid = null;

        if (merge) {
            userUid = connector.update(
                    ObjectClass.ACCOUNT, new Uid(accountId), attrs, null);
        } else {
            userUid = connector.create(
                    ObjectClass.ACCOUNT, attrs, null);
        }

        if (userUid == null) {
            if (log.isErrorEnabled()) {

                log.error(
                        "Error creating user on resource " +
                        resource.getName());

            }

            throw new IllegalStateException("Error creating user.");
        }

        if (log.isInfoEnabled()) {
            log.info("Created user " + userUid.getUidValue());
        }
    }
}