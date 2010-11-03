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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

/**
 * Error WebPage.
 */
public class ErrorPage extends BasePage {

    public ErrorPage(PageParameters parameters) {
        super(parameters);
        
        add(new Label("errorTitle",new Model<String>(
                parameters.getString("errorTitle"))));
        add(new Label("errorMessage",new Model<String>(
                parameters.getString("errorMessage"))));

        add(new BookmarkablePageLink("home", WelcomePage.class));
    }
}
