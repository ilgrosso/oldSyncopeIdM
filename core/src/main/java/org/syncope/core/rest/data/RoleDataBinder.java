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
package org.syncope.core.rest.data;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.util.AttributableUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.RVirAttr;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class RoleDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private PolicyDataBinder policyDataBinder;

    public SyncopeRole create(final RoleTO roleTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeRole role = new SyncopeRole();
        role.setInheritAttributes(roleTO.isInheritAttributes());
        role.setInheritDerivedAttributes(
                roleTO.isInheritDerivedAttributes());
        role.setInheritVirtualAttributes(
                roleTO.isInheritVirtualAttributes());

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // name and parent
        SyncopeClientException invalidRoles =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidRoles);
        if (roleTO.getName() == null) {
            LOG.error("No name specified for this role");

            invalidRoles.addElement("No name specified for this role");
        } else {
            role.setName(roleTO.getName());
        }
        Long parentRoleId = null;
        if (roleTO.getParent() != 0) {
            SyncopeRole parentRole = roleDAO.find(roleTO.getParent());
            if (parentRole == null) {
                LOG.error("Could not find role with id " + roleTO.getParent());

                invalidRoles.addElement(String.valueOf(roleTO.getParent()));
                scce.addException(invalidRoles);
            } else {
                role.setParent(parentRole);
                parentRoleId = role.getParent().getId();
            }
        }

        SyncopeRole otherRole = roleDAO.find(
                roleTO.getName(), parentRoleId);
        if (otherRole != null) {
            LOG.error("Another role exists with the same name "
                    + "and the same parent role: " + otherRole);

            invalidRoles.addElement(roleTO.getName());
        }

        // attributes, derived attributes, virtual attributes and resources
        fill(role, roleTO, AttributableUtil.ROLE, scce);

        // entitlements
        Entitlement entitlement;
        for (String entitlementName : roleTO.getEntitlements()) {
            entitlement = entitlementDAO.find(entitlementName);
            if (entitlement == null) {
                LOG.warn("Ignoring invalid entitlement {}", entitlementName);
            } else {
                role.addEntitlement(entitlement);
            }
        }

        role.setPasswordPolicy(roleTO.getPasswordPolicy() != null
                ? policyDAO.find(roleTO.getPasswordPolicy()) : null);

        return role;
    }

    public PropagationByResource update(SyncopeRole role, RoleMod roleMod)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // name
        SyncopeClientException invalidRoles = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidRoles);
        if (roleMod.getName() != null) {
            SyncopeRole otherRole = roleDAO.find(
                    roleMod.getName(),
                    role.getParent() != null ? role.getParent().getId() : 0L);

            if (otherRole != null) {
                LOG.error("Another role exists with the same name "
                        + "and the same parent role: " + otherRole);

                invalidRoles.addElement(roleMod.getName());
                scce.addException(invalidRoles);
            } else {
                role.setName(roleMod.getName());
            }
        }

        // inherited attributes
        if (roleMod.getInheritAttributes() != null) {
            role.setInheritAttributes(
                    roleMod.getInheritAttributes());
        }

        // inherited derived attributes
        if (roleMod.getInheritDerivedAttributes() != null) {
            role.setInheritDerivedAttributes(
                    roleMod.getInheritDerivedAttributes());
        }

        // inherited virtual attributes
        if (roleMod.getInheritVirtualAttributes() != null) {
            role.setInheritVirtualAttributes(
                    roleMod.getInheritVirtualAttributes());
        }

        // entitlements
        role.getEntitlements().clear();
        Entitlement entitlement;
        for (String entitlementName : roleMod.getEntitlements()) {
            entitlement = entitlementDAO.find(entitlementName);
            if (entitlement == null) {
                LOG.warn("Ignoring invalid entitlement {}", entitlementName);
            } else {
                role.addEntitlement(entitlement);
            }
        }

        role.setPasswordPolicy(
                policyDataBinder.getPolicy(roleMod.getPasswordPolicy()));

        // attributes, derived attributes, virtual attributes and resources
        return fill(role, roleMod, AttributableUtil.ROLE, scce);
    }

    public RoleTO getRoleTO(SyncopeRole role) {
        RoleTO roleTO = new RoleTO();
        roleTO.setId(role.getId());
        roleTO.setName(role.getName());
        roleTO.setInheritAttributes(role.isInheritAttributes());
        roleTO.setInheritDerivedAttributes(role.isInheritDerivedAttributes());
        roleTO.setInheritVirtualAttributes(role.isInheritVirtualAttributes());

        if (role.getParent() != null) {
            roleTO.setParent(role.getParent().getId());
        }

        // -------------------------
        // Retrieve all [derived/virtual] attributes (inherited and not)
        // -------------------------
        final List<RAttr> allAttributes = role.findInheritedAttributes();
        allAttributes.addAll((List<RAttr>) role.getAttributes());

        final List<RDerAttr> allDerAttributes =
                role.findInheritedDerivedAttributes();
        allDerAttributes.addAll((List<RDerAttr>) role.getDerivedAttributes());

        final List<RVirAttr> allVirAttributes =
                role.findInheritedVirtualAttributes();
        allVirAttributes.addAll((List<RVirAttr>) role.getVirtualAttributes());
        // -------------------------

        fillTO(roleTO,
                allAttributes,
                allDerAttributes,
                allVirAttributes,
                role.getTargetResources());

        for (Entitlement entitlement : role.getEntitlements()) {
            roleTO.addEntitlement(entitlement.getName());
        }

        roleTO.setPasswordPolicy(role.getPasswordPolicy() != null
                ? role.getPasswordPolicy().getId() : null);

        return roleTO;
    }
}
