/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.console.rest.ConfigurationRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

/**
 * Modal window with Connector form.
 */
public class ConfigurationModalPage extends BaseModalPage {

    private static final long serialVersionUID = -5266230025217580098L;

    @SpringBean
    private ConfigurationRestClient configurationsRestClient;

    private AjaxButton submit;

    /**
     * ConfigurationModalPage constructor.
     * 
     * @param basePage base
     * @param modalWindow modal-window
     * @param configurationTO
     * @param createFlag true for CREATE and false for UPDATE operation
     */
    public ConfigurationModalPage(final BasePage basePage,
            final ModalWindow window,
            final ConfigurationTO configurationTO,
            final boolean createFlag) {

        Form form = new Form("form", new CompoundPropertyModel(
                configurationTO));

        final AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                "key", "key",
                new PropertyModel(configurationTO, "key"), false);
        form.add(key);
        key.setEnabled(createFlag);
        key.addRequiredLabel();

        final AjaxTextFieldPanel value = new AjaxTextFieldPanel(
                "value", "value",
                new PropertyModel(configurationTO, "value"), false);
        form.add(value);
        value.setEnabled(createFlag);
        value.addRequiredLabel();

        submit = new IndicatingAjaxButton(
                "apply", new Model<String>(getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                boolean res = false;

                if (createFlag) {
                    res = configurationsRestClient.createConfiguration(
                            configurationTO);

                    if (!res) {
                        error(getString("error_insert"));
                    }

                } else {
                    res = configurationsRestClient.updateConfiguration(
                            configurationTO);

                    if (!res) {
                        error(getString("error_updating"));
                    }

                }

                if (res) {
                    Configuration callerPage = (Configuration) basePage;
                    callerPage.setOperationResult(true);

                    window.close(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Configuration",
                    "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Configuration",
                    "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                allowedRoles);

        form.add(submit);

        add(form);
    }
}
