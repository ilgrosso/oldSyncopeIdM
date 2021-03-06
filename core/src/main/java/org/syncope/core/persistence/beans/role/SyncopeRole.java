/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.syncope.core.persistence.beans.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PasswordPolicy;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {
    "name",
    "parent_id"
}))
@Cacheable
public class SyncopeRole extends AbstractAttributable {

    private static final long serialVersionUID = -5281258853142421875L;

    @Id
    private Long id;

    @NotNull
    private String name;

    @ManyToOne(optional = true)
    private SyncopeRole parent;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
    @JoinColumn(name = "role_id"),
    inverseJoinColumns =
    @JoinColumn(name = "entitlement_name"))
    private Set<Entitlement> entitlements;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RAttr> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RDerAttr> derivedAttributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<RVirAttr> virtualAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritDerivedAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritVirtualAttributes;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritPasswordPolicy;

    @Basic(optional = true)
    @Min(0)
    @Max(1)
    private Integer inheritAccountPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private PasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private AccountPolicy accountPolicy;

    /**
     * Provisioning external resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
    @JoinColumn(name = "role_id"),
    inverseJoinColumns =
    @JoinColumn(name = "resource_name"))
    @Valid
    private Set<ExternalResource> resources;

    public SyncopeRole() {
        super();

        entitlements = new HashSet<Entitlement>();
        attributes = new ArrayList<RAttr>();
        derivedAttributes = new ArrayList<RDerAttr>();
        virtualAttributes = new ArrayList<RVirAttr>();
        inheritAttributes = getBooleanAsInteger(false);
        inheritDerivedAttributes = getBooleanAsInteger(false);
        inheritVirtualAttributes = getBooleanAsInteger(false);
        inheritPasswordPolicy = getBooleanAsInteger(false);
        inheritAccountPolicy = getBooleanAsInteger(false);
        resources = new HashSet<ExternalResource>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    protected Set<ExternalResource> resources() {
        return resources;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SyncopeRole getParent() {
        return parent;
    }

    public void setParent(SyncopeRole parent) {
        this.parent = parent;
    }

    public boolean addEntitlement(final Entitlement entitlement) {
        return entitlements.add(entitlement);
    }

    public boolean removeEntitlement(final Entitlement entitlement) {
        return entitlements.remove(entitlement);
    }

    public Set<Entitlement> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(final List<Entitlement> entitlements) {
        this.entitlements.clear();
        if (entitlements != null && !entitlements.isEmpty()) {
            this.entitlements.addAll(entitlements);
        }
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(T attribute) {
        return attributes.add((RAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(T attribute) {
        return attributes.remove((RAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttr> attributes) {
        this.attributes = (List<RAttr>) attributes;
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((RDerAttr) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove(
                (RDerAttr) derivedAttribute);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            List<? extends AbstractDerAttr> derivedAttributes) {

        this.derivedAttributes = (List<RDerAttr>) derivedAttributes;
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirtualAttribute(
            T virtualAttribute) {

        return virtualAttributes.add((RVirAttr) virtualAttribute);
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirtualAttribute(
            T virtualAttribute) {

        return virtualAttributes.remove((RVirAttr) virtualAttribute);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirtualAttributes() {
        return virtualAttributes;
    }

    @Override
    public void setVirtualAttributes(
            List<? extends AbstractVirAttr> virtualAttributes) {

        this.virtualAttributes = (List<RVirAttr>) virtualAttributes;
    }

    public boolean isInheritAttributes() {
        return isBooleanAsInteger(inheritAttributes);
    }

    public void setInheritAttributes(boolean inheritAttributes) {
        this.inheritAttributes = getBooleanAsInteger(inheritAttributes);
    }

    /**
     * Get all inherited attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RAttr> findInheritedAttributes() {
        final Map<RSchema, RAttr> result = new HashMap<RSchema, RAttr>();

        if (isInheritAttributes() && getParent() != null) {
            final Map<AbstractSchema, AbstractAttr> attrMap = getAttrMap();

            // Add attributes not specialized
            for (RAttr attr : (Collection<RAttr>) getParent().getAttributes()) {
                if (!attrMap.containsKey(attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RAttr attr : getParent().findInheritedAttributes()) {
                if (!attrMap.containsKey(attr.getSchema())
                        && !result.containsKey((RSchema) attr.getSchema())) {
                    result.put((RSchema) attr.getSchema(), attr);
                }
            }
        }

        return new ArrayList<RAttr>(result.values());
    }

    public boolean isInheritDerivedAttributes() {
        return isBooleanAsInteger(inheritDerivedAttributes);
    }

    public void setInheritDerivedAttributes(boolean inheritDerivedAttributes) {
        this.inheritDerivedAttributes =
                getBooleanAsInteger(inheritDerivedAttributes);

    }

    /**
     * Get all inherited derived attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RDerAttr> findInheritedDerivedAttributes() {
        final Map<RDerSchema, RDerAttr> result =
                new HashMap<RDerSchema, RDerAttr>();

        if (isInheritDerivedAttributes() && getParent() != null) {
            final Map<AbstractDerSchema, AbstractDerAttr> attrMap =
                    getDerAttrMap();

            // Add attributes not specialized
            for (RDerAttr attr :
                    (Collection<RDerAttr>) getParent().getDerivedAttributes()) {
                if (!attrMap.containsKey(attr.getDerivedSchema())) {
                    result.put((RDerSchema) attr.getDerivedSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RDerAttr attr : getParent().findInheritedDerivedAttributes()) {
                if (!attrMap.containsKey(attr.getDerivedSchema())
                        && !result.containsKey(
                        (RDerSchema) attr.getDerivedSchema())) {
                    result.put((RDerSchema) attr.getDerivedSchema(), attr);
                }
            }
        }

        return new ArrayList<RDerAttr>(result.values());
    }

    public boolean isInheritVirtualAttributes() {
        return isBooleanAsInteger(inheritVirtualAttributes);
    }

    public void setInheritVirtualAttributes(boolean inheritVirtualAttributes) {
        this.inheritVirtualAttributes =
                getBooleanAsInteger(inheritVirtualAttributes);

    }

    /**
     * Get all inherited virtual attributes from the ancestors.
     *
     * @return a list of inherited and only inherited attributes.
     */
    public List<RVirAttr> findInheritedVirtualAttributes() {
        final Map<RVirSchema, RVirAttr> result =
                new HashMap<RVirSchema, RVirAttr>();

        if (isInheritVirtualAttributes() && getParent() != null) {
            final Map<AbstractVirSchema, AbstractVirAttr> attrMap =
                    getVirAttrMap();

            // Add attributes not specialized
            for (RVirAttr attr :
                    (Collection<RVirAttr>) getParent().getVirtualAttributes()) {
                if (!attrMap.containsKey(attr.getVirtualSchema())) {
                    result.put((RVirSchema) attr.getVirtualSchema(), attr);
                }
            }

            // Add attributes not specialized and not already added
            for (RVirAttr attr : getParent().findInheritedVirtualAttributes()) {
                if (!attrMap.containsKey(attr.getVirtualSchema())
                        && !result.containsKey(
                        (RVirSchema) attr.getVirtualSchema())) {
                    result.put((RVirSchema) attr.getVirtualSchema(), attr);
                }
            }
        }

        return new ArrayList<RVirAttr>(result.values());
    }

    /**
     * Get first valid password policy.
     *
     * @return parent password policy if isInheritPasswordPolicy is 'true' and
     * parent is not null. Return local passowrd policy otherwise.
     */
    public PasswordPolicy getPasswordPolicy() {
        return isInheritPasswordPolicy() && getParent() != null
                ? getParent().getPasswordPolicy() : passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isInheritPasswordPolicy() {
        return isBooleanAsInteger(inheritPasswordPolicy);
    }

    public void setInheritPasswordPolicy(boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = getBooleanAsInteger(inheritPasswordPolicy);
    }

    /**
     * Get first valid account policy.
     *
     * @return parent account policy if isInheritAccountPolicy is 'true' and
     * parent is not null. Return local account policy otherwise.
     */
    public AccountPolicy getAccountPolicy() {
        return isInheritAccountPolicy() && getParent() != null
                ? getParent().getAccountPolicy() : accountPolicy;
    }

    public void setAccountPolicy(AccountPolicy accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public boolean isInheritAccountPolicy() {
        return isBooleanAsInteger(inheritAccountPolicy);
    }

    public void setInheritAccountPolicy(boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = getBooleanAsInteger(inheritAccountPolicy);
    }
}
