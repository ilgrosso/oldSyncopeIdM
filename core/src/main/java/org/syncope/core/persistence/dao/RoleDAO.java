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
package org.syncope.core.persistence.dao;

import java.util.List;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface RoleDAO extends DAO {

    SyncopeRole find(Long id);

    List<SyncopeRole> find(String name);

    SyncopeRole find(String name, Long parent);

    List<SyncopeRole> findByEntitlement(final Entitlement entitlement);

    List<SyncopeRole> findByResource(ExternalResource resource);
    
    List<SyncopeRole> findChildren(Long roleId);

    List<SyncopeRole> findAll();

    List<Membership> findMemberships(SyncopeRole role);

    SyncopeRole save(SyncopeRole syncopeRole)
            throws InvalidEntityException;

    void delete(Long id);
}
