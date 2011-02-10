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
package org.syncope.console.wicket.markup.html.form;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.model.IModel;

/**
 * Extension class of AutoCompleteTextField. It's purposed for storing values in
 * the corresponding property model after pressing 'Add' button.
 */
public abstract class UpdatingAutoCompleteTextField
        extends AutoCompleteTextField {

    public UpdatingAutoCompleteTextField(final String id, final IModel model) {
        super(id, model);
        add(new AjaxFormComponentUpdatingBehavior("onblur") {

            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
    }
}
