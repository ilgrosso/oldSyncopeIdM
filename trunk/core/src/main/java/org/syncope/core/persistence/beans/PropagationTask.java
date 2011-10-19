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
package org.syncope.core.persistence.beans;

import com.thoughtworks.xstream.XStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Type;
import org.identityconnectors.framework.common.objects.Attribute;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.validation.entity.PropagationTaskCheck;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;

/**
 * Encapsulate all information about a propagation task.
 */
@Entity
@PropagationTaskCheck
public class PropagationTask extends Task {

    private static final long serialVersionUID = 7086054884614511210L;

    /**
     * @see PropagationMode
     */
    @Enumerated(EnumType.STRING)
    private PropagationMode propagationMode;

    /**
     * @see ResourceOperationType
     */
    @Enumerated(EnumType.STRING)
    private PropagationOperation resourceOperationType;

    /**
     * The accountId on the external resource.
     */
    private String accountId;

    /**
     * The (optional) former accountId on the external resource.
     */
    private String oldAccountId;

    /**
     * Attributes to be propagated.
     */
    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String xmlAttributes;

    /**
     * ExternalResource to which the propagation happens.
     */
    @ManyToOne
    private ExternalResource resource;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public Set<Attribute> getAttributes() {
        Set<Attribute> result = Collections.EMPTY_SET;

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = context.getBean(XStream.class);
        try {
            result = (Set<Attribute>) xStream.fromXML(
                    URLDecoder.decode(xmlAttributes, "UTF-8"));
        } catch (Throwable t) {
            LOG.error("During attribute deserialization", t);
        }

        return result;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = context.getBean(XStream.class);
        try {
            xmlAttributes = URLEncoder.encode(
                    xStream.toXML(attributes), "UTF-8");
        } catch (Throwable t) {
            LOG.error("During attribute serialization", t);
        }
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public PropagationOperation getResourceOperationType() {
        return resourceOperationType;
    }

    public void setResourceOperationType(
            final PropagationOperation resourceOperationType) {

        this.resourceOperationType = resourceOperationType;
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(ExternalResource resource) {
        this.resource = resource;
    }
}