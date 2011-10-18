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

import org.activiti.engine.delegate.DelegateExecution;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.workflow.ActivitiUserWorkflowAdapter;

public class Delete extends AbstractActivitiDelegate {

    @Override
    protected void doExecute(final DelegateExecution execution)
            throws Exception {

        SyncopeUser user = (SyncopeUser) execution.getVariable(
                ActivitiUserWorkflowAdapter.SYNCOPE_USER);

        // delete SyncopeUser
        CONTEXT.getBean(UserDAO.class).delete(user);

        // remove SyncopeUser variable
        execution.removeVariable(ActivitiUserWorkflowAdapter.SYNCOPE_USER);
    }
}