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
package org.syncope.client.to;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class UserTO extends AbstractBaseTO {

    private long id;
    private String password;
    private Set<Long> roles;
    private Set<AttributeTO> attributes;
    private Set<AttributeTO> derivedAttributes;
    private Set<String> resources;
    private Date creationTime;
    private String token;
    private Date tokenExpireTime;

    public UserTO() {
        roles = new HashSet<Long>();
        attributes = new HashSet<AttributeTO>();
        derivedAttributes = new HashSet<AttributeTO>();
        resources = new HashSet<String>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean addRole(Long role) {
        return roles.add(role);
    }

    public boolean removeRole(Long role) {
        return roles.remove(role);
    }

    public Set<Long> getRoles() {
        return roles;
    }

    public void setRoles(Set<Long> roles) {
        this.roles = roles;
    }

    public boolean addAttribute(AttributeTO attribute) {
        return attributes.add(attribute);
    }

    public boolean removeAttribute(AttributeTO attribute) {
        return attributes.remove(attribute);
    }

    public Set<AttributeTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AttributeTO> attributes) {
        this.attributes = attributes;
    }

    public boolean addDerivedAttribute(AttributeTO derivedAttribute) {
        return derivedAttributes.add(derivedAttribute);
    }

    public boolean removeDerivedAttribute(AttributeTO derivedAttribute) {
        return derivedAttributes.remove(derivedAttribute);
    }

    public Set<AttributeTO> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(Set<AttributeTO> derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }

    public boolean addResource(String resource) {
        return resources.add(resource);
    }

    public boolean removeResource(String resource) {
        return resources.remove(resource);
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(Set<String> resources) {
        this.resources = resources;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(Date tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }
}
