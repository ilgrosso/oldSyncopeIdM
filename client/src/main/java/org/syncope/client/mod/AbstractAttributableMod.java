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
package org.syncope.client.mod;

import java.util.HashSet;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;

public abstract class AbstractAttributableMod extends AbstractBaseBean {

    protected long id;

    protected Set<AttributeMod> attributesToBeUpdated;

    protected Set<String> attributesToBeRemoved;

    protected Set<String> derivedAttributesToBeAdded;

    protected Set<String> derivedAttributesToBeRemoved;

    protected Set<String> virtualAttributesToBeAdded;

    protected Set<String> virtualAttributesToBeRemoved;

    protected Set<String> resourcesToBeAdded;

    protected Set<String> resourcesToBeRemoved;

    public AbstractAttributableMod() {
        attributesToBeUpdated = new HashSet<AttributeMod>();
        attributesToBeRemoved = new HashSet<String>();
        derivedAttributesToBeAdded = new HashSet<String>();
        derivedAttributesToBeRemoved = new HashSet<String>();
        virtualAttributesToBeAdded = new HashSet<String>();
        virtualAttributesToBeRemoved = new HashSet<String>();
        resourcesToBeAdded = new HashSet<String>();
        resourcesToBeRemoved = new HashSet<String>();
    }

    public boolean addAttributeToBeRemoved(String attribute) {
        return attributesToBeRemoved.add(attribute);
    }

    public boolean removeAttributeToBeRemoved(String attribute) {
        return attributesToBeRemoved.remove(attribute);
    }

    public Set<String> getAttributesToBeRemoved() {
        return attributesToBeRemoved;
    }

    public void setAttributesToBeRemoved(Set<String> attributesToBeRemoved) {
        this.attributesToBeRemoved = attributesToBeRemoved;
    }

    public boolean addAttributeToBeUpdated(AttributeMod attribute) {
        return attributesToBeUpdated.add(attribute);
    }

    public boolean removeAttributeToBeUpdated(AttributeMod attribute) {
        return attributesToBeUpdated.remove(attribute);
    }

    public Set<AttributeMod> getAttributesToBeUpdated() {
        return attributesToBeUpdated;
    }

    public void setAttributesToBeUpdated(
            Set<AttributeMod> attributesToBeUpdated) {

        this.attributesToBeUpdated = attributesToBeUpdated;
    }

    public boolean addDerivedAttributeToBeAdded(String derivedAttribute) {
        return derivedAttributesToBeAdded.add(derivedAttribute);
    }

    public boolean removeDerivedAttributeToBeAdded(String derivedAttribute) {
        return derivedAttributesToBeAdded.remove(derivedAttribute);
    }

    public Set<String> getDerivedAttributesToBeAdded() {
        return derivedAttributesToBeAdded;
    }

    public void setDerivedAttributesToBeAdded(
            Set<String> derivedAttributesToBeAdded) {

        this.derivedAttributesToBeAdded = derivedAttributesToBeAdded;
    }

    public boolean addDerivedAttributeToBeRemoved(String derivedAttribute) {
        return derivedAttributesToBeRemoved.add(derivedAttribute);
    }

    public boolean removeDerivedAttributeToBeRemoved(String derivedAttribute) {
        return derivedAttributesToBeRemoved.remove(derivedAttribute);
    }

    public Set<String> getDerivedAttributesToBeRemoved() {
        return derivedAttributesToBeRemoved;
    }

    public void setDerivedAttributesToBeRemoved(
            Set<String> derivedAttributesToBeRemoved) {

        this.derivedAttributesToBeRemoved = derivedAttributesToBeRemoved;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean addResourceToBeAdded(String resource) {
        return resourcesToBeAdded.add(resource);
    }

    public boolean removeResourceToBeAdded(String resource) {
        return resourcesToBeAdded.remove(resource);
    }

    public Set<String> getResourcesToBeAdded() {
        return resourcesToBeAdded;
    }

    public void setResourcesToBeAdded(Set<String> resourcesToBeAdded) {
        this.resourcesToBeAdded = resourcesToBeAdded;
    }

    public boolean addResourceToBeRemoved(String resource) {
        return resourcesToBeRemoved.add(resource);
    }

    public boolean removeResourceToBeRemoved(String resource) {
        return resourcesToBeRemoved.remove(resource);
    }

    public Set<String> getResourcesToBeRemoved() {
        return resourcesToBeRemoved;
    }

    public void setResourcesToBeRemoved(Set<String> resourcesToBeRemoved) {
        this.resourcesToBeRemoved = resourcesToBeRemoved;
    }

    public Set<String> getVirtualAttributesToBeAdded() {
        return virtualAttributesToBeAdded;
    }

    public void setVirtualAttributesToBeAdded(Set<String> virtualAttributesToBeAdded) {
        this.virtualAttributesToBeAdded = virtualAttributesToBeAdded;
    }

    public Set<String> getVirtualAttributesToBeRemoved() {
        return virtualAttributesToBeRemoved;
    }

    public void setVirtualAttributesToBeRemoved(Set<String> virtualAttributesToBeRemoved) {
        this.virtualAttributesToBeRemoved = virtualAttributesToBeRemoved;
    }
}
