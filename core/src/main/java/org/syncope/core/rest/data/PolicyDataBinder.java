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
package org.syncope.core.rest.data;

import org.springframework.stereotype.Component;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.types.AccountPolicySpec;
import org.syncope.types.PasswordPolicySpec;
import org.syncope.types.SyncPolicySpec;

@Component
public class PolicyDataBinder {

    /**
     * Get policy TO from policy bean.
     * @param policy bean.
     * @return policy TO.
     */
    public <T extends PolicyTO> T getPolicyTO(final Policy policy) {
        final PolicyTO policyTO;
        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
                policyTO = new PasswordPolicyTO(true);
                ((PasswordPolicyTO) policyTO).setSpecification(
                        (PasswordPolicySpec) policy.getSpecification());
                break;

            case PASSWORD:
                policyTO = new PasswordPolicyTO();
                ((PasswordPolicyTO) policyTO).setSpecification(
                        (PasswordPolicySpec) policy.getSpecification());
                break;

            case GLOBAL_ACCOUNT:
                policyTO = new AccountPolicyTO(true);
                ((AccountPolicyTO) policyTO).setSpecification(
                        (AccountPolicySpec) policy.getSpecification());
                break;

            case ACCOUNT:
                policyTO = new AccountPolicyTO();
                ((AccountPolicyTO) policyTO).setSpecification(
                        (AccountPolicySpec) policy.getSpecification());
                break;

            case GLOBAL_SYNC:
                policyTO = new SyncPolicyTO(true);
                ((SyncPolicyTO) policyTO).setSpecification(
                        (SyncPolicySpec) policy.getSpecification());
                break;

            case SYNC:
            default:
                policyTO = new SyncPolicyTO();
                ((SyncPolicyTO) policyTO).setSpecification(
                        (SyncPolicySpec) policy.getSpecification());

        }

        policyTO.setId(policy.getId());
        policyTO.setDescription(policy.getDescription());

        return (T) policyTO;
    }

    public <T extends Policy> T getPolicy(
            T policy, final PolicyTO policyTO) {

        if (policy != null && policy.getType() != policyTO.getType()) {
            throw new IllegalArgumentException(
                    String.format("Cannot update %s from %s",
                    policy.getType(), policyTO.getType()));
        }

        switch (policyTO.getType()) {
            case GLOBAL_PASSWORD:
                if (policy == null) {
                    policy = (T) new PasswordPolicy(true);
                }
                policy.setSpecification(
                        ((PasswordPolicyTO) policyTO).getSpecification());
                break;

            case PASSWORD:
                if (policy == null) {
                    policy = (T) new PasswordPolicy();
                }
                policy.setSpecification(
                        ((PasswordPolicyTO) policyTO).getSpecification());
                break;

            case GLOBAL_ACCOUNT:
                if (policy == null) {
                    policy = (T) new AccountPolicy(true);
                }
                policy.setSpecification(
                        ((AccountPolicyTO) policyTO).getSpecification());
                break;

            case ACCOUNT:
                if (policy == null) {
                    policy = (T) new AccountPolicy();
                }
                policy.setSpecification(
                        ((AccountPolicyTO) policyTO).getSpecification());
                break;

            case GLOBAL_SYNC:
                if (policy == null) {
                    policy = (T) new SyncPolicy(true);
                }
                policy.setSpecification(
                        ((SyncPolicyTO) policyTO).getSpecification());
                break;

            case SYNC:
            default:
                if (policy == null) {
                    policy = (T) new SyncPolicy();
                }
                policy.setSpecification(
                        ((SyncPolicyTO) policyTO).getSpecification());
        }

        policy.setDescription(policyTO.getDescription());

        return policy;
    }
}
