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
package org.syncope.core.persistence.beans.user;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
public class UserDerivedSchema extends AbstractDerivedSchema {

    @ManyToMany
    private Set<UserSchema> schemas;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "derivedSchema")
    private Set<UserDerivedAttribute> derivedAttributes;

    public UserDerivedSchema() {
        schemas = new HashSet<UserSchema>();
        derivedAttributes = new HashSet<UserDerivedAttribute>();
    }

    @Override
    public <T extends AbstractSchema> boolean addSchema(T schema) {
        return schemas.add((UserSchema) schema);
    }

    @Override
    public <T extends AbstractSchema> boolean removeSchema(T schema) {
        return schemas.remove((UserSchema) schema);
    }

    @Override
    public Set<UserSchema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Set<? extends AbstractSchema> schemas) {
        this.schemas = (Set<UserSchema>) schemas;
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((UserDerivedAttribute) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((UserDerivedAttribute) derivedAttribute);
    }

    @Override
    public Set<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public <T extends AbstractDerivedAttribute> void setDerivedAttributes(
            Set<T> derivedAttributes) {

        this.derivedAttributes = (Set<UserDerivedAttribute>) derivedAttributes;
    }
}
