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
package org.syncope.console.wicket.markup.html.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IRenderable;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyRenderableColumn;
import org.apache.wicket.model.PropertyModel;
import org.syncope.client.to.RoleTO;

public class PropertyEditableColumn extends PropertyRenderableColumn {

    private static final long serialVersionUID = -6279911884966836754L;

    private ModalWindow window = null;

    private PageReference callerPageRef;

    public PropertyEditableColumn(final ColumnLocation location,
            final String header, final String propertyExpression,
            final ModalWindow window, final PageReference callerPageRef) {

        super(location, header, propertyExpression);

        this.callerPageRef = callerPageRef;
        this.window = window;
    }

    @Override
    public Component newCell(final MarkupContainer parent, final String id,
            final TreeNode node, final int level) {

        DefaultMutableTreeNode syncopeTreeNode = (DefaultMutableTreeNode) node;
        RoleTO roleTO = (RoleTO) syncopeTreeNode.getUserObject();

        NodeEditablePanel editablePanel = new NodeEditablePanel(id,
                roleTO.getId(),
                new PropertyModel(node, getPropertyExpression()),
                window, callerPageRef);

        return editablePanel;
    }

    @Override
    public IRenderable newCell(final TreeNode node, final int level) {
        return getTreeTable().getTreeState().isNodeSelected(node)
                ? null : super.newCell(node, level);
    }
}
