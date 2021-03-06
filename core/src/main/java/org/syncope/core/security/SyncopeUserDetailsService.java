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
package org.syncope.core.security;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;

@Configurable
public class SyncopeUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    private String adminUser;

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    @Override
    public UserDetails loadUserByUsername(final String username)
            throws UsernameNotFoundException, DataAccessException {

        Set<SimpleGrantedAuthority> authorities =
                new HashSet<SimpleGrantedAuthority>();
        if (adminUser.equals(username)) {
            for (Entitlement entitlement : entitlementDAO.findAll()) {
                authorities.add(
                        new SimpleGrantedAuthority(entitlement.getName()));
            }
        } else {
            final SyncopeUser user = userDAO.find(username);

            if (user == null) {
                throw new UsernameNotFoundException(
                        "Could not find any user with id " + username);
            }

            // Give entitlements based on roles owned by user,
            // considering role inheritance as well
            Set<SyncopeRole> roles = new HashSet<SyncopeRole>(user.getRoles());
            for (Long roleId : user.getRoleIds()) {
                roles.addAll(roleDAO.findChildren(roleId));
            }
            for (SyncopeRole role : roles) {
                for (Entitlement entitlement : role.getEntitlements()) {
                    authorities.add(
                            new SimpleGrantedAuthority(entitlement.getName()));
                }
            }
        }

        return new User(username, "<PASSWORD_PLACEHOLDER>",
                true, true, true, true, authorities);
    }
}
