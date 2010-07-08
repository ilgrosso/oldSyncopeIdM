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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.UsersRestClient;

/**
 * Users WebPage.
 */
public class Users extends BasePage {

    @SpringBean(name = "usersRestClient")
    UsersRestClient restClient;

    public Users(PageParameters parameters) {
        super(parameters);

        add(new TextField("search", new Model(getString("search"))));

        //add(new Button("newUserBtn", new Model(getString("newUserBtn"))));

        final Set<UserTO> users = restClient.getUserList();

        if (!users.isEmpty()) {

            final List userList = new ArrayList(users);

            /*add(new ListView("userList", userList) {
            @Override
            protected void populateItem(ListItem item) {
            //item.add(new Label("label", item.getModel()));
            }

            });*/

        }

    }
}