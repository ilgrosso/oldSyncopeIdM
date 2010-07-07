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

import java.util.HashSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import org.syncope.core.persistence.beans.Resource;

@MappedSuperclass
public abstract class AbstractAttributable extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Provisioning target resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Resource> resources;

    public AbstractAttributable() {
        resources = new HashSet<Resource>();
    }

    public Long getId() {
        return id;
    }

    public <T extends AbstractAttribute> T getAttribute(String schemaName) {
        T result = null;
        T attribute = null;
        for (Iterator<? extends AbstractAttribute> itor =
                getAttributes().iterator();
                result == null && itor.hasNext();) {

            attribute = (T) itor.next();
            if (attribute.getSchema() != null && schemaName.equals(attribute.getSchema().getName())) {

                result = attribute;
            }
        }

        return result;
    }

    public <T extends AbstractDerivedAttribute> T getDerivedAttribute(
            String derivedSchemaName) throws NoSuchElementException {

        T result = null;
        T derivedAttribute = null;
        for (Iterator<? extends AbstractDerivedAttribute> itor =
                getDerivedAttributes().iterator();
                result == null && itor.hasNext();) {

            derivedAttribute = (T) itor.next();
            if (derivedAttribute.getDerivedSchema() != null && derivedSchemaName.equals(
                    derivedAttribute.getDerivedSchema().getName())) {

                result = derivedAttribute;
            }
        }

        return result;
    }

    public boolean addResource(Resource resource) {
        return resources.add(resource);
    }

    public boolean removeResource(Resource resource) {
        return resources.remove(resource);
    }

    public Set<Resource> getResources() {
        if (this.resources == null) this.resources = new HashSet<Resource>();
        return this.resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public abstract <T extends AbstractAttribute> boolean addAttribute(T attribute);

    public abstract <T extends AbstractAttribute> boolean removeAttribute(T attribute);

    public abstract Set<? extends AbstractAttribute> getAttributes();

    public abstract void setAttributes(
            Set<? extends AbstractAttribute> attributes);

    public abstract <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(T derivedAttribute);

    public abstract <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(T derivedAttribute);

    public abstract Set<? extends AbstractDerivedAttribute> getDerivedAttributes();

    public abstract void setDerivedAttributes(
            Set<? extends AbstractDerivedAttribute> derivedAttributes);
}
