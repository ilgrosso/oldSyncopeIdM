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
package org.syncope.core.persistence.dao;

import java.util.List;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.types.PolicyType;

public interface PolicyDAO extends DAO {

    Policy find(Long id);

    Policy getGlobalPasswordPolicy();

    List<Policy> find(PolicyType type);

    Policy getGlobalAccountPolicy();

    Policy getGlobalSyncPolicy();

    List<Policy> findAll();

    Policy save(Policy policy);

    void delete(Long id);
}