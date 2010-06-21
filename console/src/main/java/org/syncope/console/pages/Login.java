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

package org.syncope.console.pages;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    public Form form;
    public TextField usernameField;
    public TextField passwordField;
    public DropDownChoice<String> languageSelect;

    public Login(PageParameters parameters) {
        super(parameters);

        form = new Form("login");

        usernameField = new TextField("username", new Model());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language", Arrays
        .asList(new Locale[] { Locale.ENGLISH, Locale.ITALIAN }));

        form.add(languageSelect);

        Button submitButton = new Button("submit", new Model(getString("submit"))) {
            @Override
            public void onSubmit() {
                System.out.println("Submit");
                setResponsePage(new HomePage(null));
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        form.add(new Button("reset", new Model(getString("reset"))));

        add(form);
    }

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    public class LocaleDropDown extends DropDownChoice {

        private class LocaleRenderer extends ChoiceRenderer {

            @Override
            public String getDisplayValue(Object locale) {
                return ((Locale) locale).getDisplayName(getLocale());
            }
        }

        public LocaleDropDown(String id, List<Locale> supportedLocales) {
            super(id, supportedLocales);
            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel() {

                public Object getObject() {

                    return getSession().getLocale();

                }

                public void setObject(Object object) {
                    getSession().setLocale((Locale) object);
                }

                public void detach() {
                }
            });
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications() {
            return true;
        }
    }
}