/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow.activiti;

import org.activiti.engine.impl.variable.SerializableType;
import org.syncope.core.persistence.beans.AbstractBaseBean;

/**
 * Activiti variable type for handling Syncope entities as Activiti variables.
 * Main purpose: avoid Activiti to handle Syncope entities as JPA entities,
 * since this can cause troubles with transactions.
 */
public class SyncopeEntitiesVariableType extends SerializableType {

    @Override
    public boolean isAbleToStore(final Object value) {
        return value instanceof AbstractBaseBean;
    }
}
