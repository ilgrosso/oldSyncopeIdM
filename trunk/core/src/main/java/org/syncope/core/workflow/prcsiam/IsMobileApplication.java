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
package org.syncope.core.workflow.prcsiam;

import org.syncope.core.workflow.OSWorkflowComponent;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Condition;
import com.opensymphony.workflow.WorkflowException;
import java.util.Map;
import org.syncope.core.persistence.beans.role.RoleAttribute;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.Constants;

public class IsMobileApplication extends OSWorkflowComponent
        implements Condition {

    @Override
    public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);
        boolean mobileApplication = false;

        RoleAttribute attribute = null;
        for (SyncopeRole application : syncopeUser.getRoles()) {
            attribute = application.getAttribute("applicationType");
            if (attribute != null && !attribute.getAttributeValues().isEmpty()) {
                mobileApplication = "mobile".equals(
                        attribute.getAttributeValues().iterator().next().getStringValue());
            }
        }

        return mobileApplication;
    }
}
