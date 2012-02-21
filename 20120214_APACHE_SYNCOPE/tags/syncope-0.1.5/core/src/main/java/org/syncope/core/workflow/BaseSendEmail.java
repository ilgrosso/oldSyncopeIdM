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
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;

public class BaseSendEmail extends OSWorkflowComponent
        implements FunctionProvider {

    protected SyncopeConfigurationDAO syncopeConfigurationDAO;

    public BaseSendEmail() {
        syncopeConfigurationDAO =
                (SyncopeConfigurationDAO) context.getBean(
                "syncopeConfigurationDAOImpl");
    }

    protected String getEmailBody(String urlPrefix, String template,
            String urlSuffix, String fallback) {

        String templateURL = urlPrefix + template + urlSuffix;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Email template URL: " + templateURL);
        }

        StringBuilder templateContent = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new URL(templateURL).openStream()));

            String data = null;
            while ((data = reader.readLine()) != null) {
                templateContent.append(data);
            }

            reader.close();
        } catch (Exception e) {
            LOG.error("While reading mail template " + template, e);
        }

        return templateContent.length() == 0
                ? fallback : templateContent.toString();
    }

    protected HtmlEmail getHtmlEmail(Map<String, Object> transientVars,
            Map<String, String> args, PropertySet ps)
            throws EmailException, WorkflowException {

        HtmlEmail email = new HtmlEmail();
        try {
            email.setHostName(syncopeConfigurationDAO.find(
                    "smtp.host").getConfValue());
            email.setFrom(syncopeConfigurationDAO.find(
                    args.get("from")).getConfValue());
            email.setSubject(syncopeConfigurationDAO.find(
                    args.get("subject")).getConfValue());
        } catch (MissingConfKeyException e) {
            throw new WorkflowException(e);
        }

        return email;
    }

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        Email email = null;
        try {
            email = getHtmlEmail(transientVars, args, ps);
            email.send();
        } catch (EmailException e) {
            LOG.error("Could not send e-mail " + email, e);
        }
    }
}