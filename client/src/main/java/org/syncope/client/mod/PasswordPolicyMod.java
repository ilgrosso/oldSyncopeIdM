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

import org.syncope.types.PasswordPolicy;

public class PasswordPolicyMod extends PolicyMod {

    private static final long serialVersionUID = -7948423277026280828L;

    private PasswordPolicy specification;

    public PasswordPolicyMod() {
    }

    public void setSpecification(PasswordPolicy specification) {
        this.specification = specification;
    }

    public PasswordPolicy getSpecification() {
        return specification;
    }
}