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
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.rest.data.RoleDataBinder;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController {

    @Autowired
    private RoleDAO syncopeRoleDAO;

    @Autowired
    private RoleDataBinder roleDataBinder;

    @PreAuthorize("hasRole('ROLE_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public RoleTO create(HttpServletResponse response,
            @RequestBody RoleTO roleTO)
            throws SyncopeClientCompositeErrorException {

        LOG.debug("Role create called with parameters {}", roleTO);

        SyncopeRole role;
        try {
            role = roleDataBinder.create(roleTO);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("Could not create for " + roleTO, e);

            throw e;
        }
        role = syncopeRoleDAO.save(role);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return roleDataBinder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{roleId}")
    public void delete(@PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);
        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException("Role " + String.valueOf(roleId));
        }

        syncopeRoleDAO.delete(roleId);
    }

    @PreAuthorize("hasRole('ROLE_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<RoleTO> list() {
        List<SyncopeRole> roles = syncopeRoleDAO.findAll();
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/parent/{roleId}")
    public RoleTO parent(@PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);
        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException("Role " + String.valueOf(roleId));
        }

        return role.getParent() == null ? null
                : roleDataBinder.getRoleTO(role.getParent());
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/children/{roleId}")
    public List<RoleTO> children(@PathVariable("roleId") Long roleId) {

        List<SyncopeRole> roles = syncopeRoleDAO.findChildren(roleId);
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{roleId}")
    public RoleTO read(@PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);
        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException(String.valueOf(roleId));
        }

        return roleDataBinder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public RoleTO update(@RequestBody RoleMod roleMod)
            throws NotFoundException {

        LOG.debug("Role update called with parameter {}", roleMod);

        SyncopeRole role = syncopeRoleDAO.find(roleMod.getId());
        if (role == null) {
            LOG.error("Could not find role '" + roleMod.getId() + "'");

            throw new NotFoundException(
                    "Role " + String.valueOf(roleMod.getId()));
        }

        roleDataBinder.update(role, roleMod);
        role = syncopeRoleDAO.save(role);

        return roleDataBinder.getRoleTO(role);
    }
}