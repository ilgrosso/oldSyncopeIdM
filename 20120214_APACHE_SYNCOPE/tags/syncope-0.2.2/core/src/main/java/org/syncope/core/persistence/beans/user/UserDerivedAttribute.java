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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;

@Entity
public class UserDerivedAttribute extends AbstractDerivedAttribute {

    @ManyToOne
    private SyncopeUser owner;
    @ManyToOne(fetch = FetchType.EAGER)
    UserDerivedSchema derivedSchema;

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(T owner) {
        this.owner = (SyncopeUser) owner;
    }

    @Override
    public <T extends AbstractDerivedSchema> T getDerivedSchema() {
        return (T) derivedSchema;
    }

    @Override
    public <T extends AbstractDerivedSchema> void setDerivedSchema(
            T derivedSchema) {

        this.derivedSchema = (UserDerivedSchema) derivedSchema;
    }
}
