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

import org.apache.wicket.PageParameters;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.SyncopeSession;
import org.syncope.console.commons.XMLRolesReader;

/**
 * Welcome page to display after successful login.
 */
public class WelcomePage extends WebPage {

    @SpringBean(name = "xmlRolesReader")
    protected XMLRolesReader xmlRolesReader;

    public WelcomePage(PageParameters parameters) {
        super(parameters);

        BookmarkablePageLink schemaLink = new BookmarkablePageLink("schema",
                Schema.class);

        String allowedSchemaRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(schemaLink, ENABLE,
                        allowedSchemaRoles);

        add(schemaLink);

        BookmarkablePageLink usersLink = new BookmarkablePageLink("users",
                Users.class);

        String allowedUsersRoles = xmlRolesReader.getAllAllowedRoles("Users",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(usersLink, ENABLE,
                        allowedUsersRoles);

        add(usersLink);

        BookmarkablePageLink rolesLink= new BookmarkablePageLink("roles",
                Roles.class);

        String allowedRoleRoles = xmlRolesReader.getAllAllowedRoles("Roles",
                        "list");

        MetaDataRoleAuthorizationStrategy.authorize(rolesLink, ENABLE,
                        allowedRoleRoles);

        add(rolesLink);

        BookmarkablePageLink resourcesLink = new BookmarkablePageLink(
                "resources", Resources.class);

        String allowedResourcesRoles = xmlRolesReader.getAllAllowedRoles(
                "Resources","list");

        MetaDataRoleAuthorizationStrategy.authorize(resourcesLink, ENABLE,
                        allowedResourcesRoles);

        add(resourcesLink);

        BookmarkablePageLink connectorsLink =
                new BookmarkablePageLink("connectors", Connectors.class);

        String allowedConnectorsRoles = xmlRolesReader.getAllAllowedRoles(
                "Connectors","list");

        MetaDataRoleAuthorizationStrategy.authorize(connectorsLink, ENABLE,
                        allowedConnectorsRoles);

        add(connectorsLink);

        BookmarkablePageLink reportLink = new BookmarkablePageLink(
                "report", Report.class);

        String allowedReportRoles = xmlRolesReader.getAllAllowedRoles(
                "Report","list");

        MetaDataRoleAuthorizationStrategy.authorize(reportLink, ENABLE,
                        allowedReportRoles);

        add(reportLink);

        BookmarkablePageLink configurationLink = new BookmarkablePageLink(
                "configuration", Configuration.class);

        String allowedConfigurationRoles = xmlRolesReader.getAllAllowedRoles(
                "Configuration","list");

        MetaDataRoleAuthorizationStrategy.authorize(configurationLink, ENABLE,
                        allowedConfigurationRoles);

        add(configurationLink);

        BookmarkablePageLink taskLink = new BookmarkablePageLink("task",
                Tasks.class);

        String allowedTasksRoles = xmlRolesReader.getAllAllowedRoles(
                "Tasks","list");

        MetaDataRoleAuthorizationStrategy.authorize(taskLink, ENABLE,
                        allowedTasksRoles);

        add(taskLink);

        add(new BookmarkablePageLink("logout", Logout.class));

        SyncopeSession session = (SyncopeSession) getSession();

        add(new Label("username",new Model<String>(session.getUser()
                .getUsername())));

    }
}
