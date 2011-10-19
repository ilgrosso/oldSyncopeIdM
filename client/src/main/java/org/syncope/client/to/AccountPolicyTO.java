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

import org.syncope.types.AccountPolicySpec;
import org.syncope.types.PolicyType;

public class AccountPolicyTO extends PolicyTO {

    private static final long serialVersionUID = -1557150042828800134L;

    private AccountPolicySpec specification;

    public AccountPolicyTO() {
        setType(PolicyType.ACCOUNT);
    }

    public void setSpecification(final AccountPolicySpec specification) {
        this.specification = specification;
    }

    public AccountPolicySpec getSpecification() {
        return specification;
    }
}
