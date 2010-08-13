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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.ResourceTOs;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.client.to.UserTO;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.rest.RolesRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UsersRestClient;

import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.console.wicket.markup.html.form.ListMultipleChoiceTransfer;
import org.syncope.console.wicket.markup.html.tree.SyncopeRoleTree;
import org.syncope.console.wicket.markup.html.tree.TreeModelBean;

/**
 * Modal window with User form.
 */
public class UserModalPage extends SyncopeModalPage {

    @SpringBean(name = "usersRestClient")
    UsersRestClient usersRestClient;

    @SpringBean(name = "rolesRestClient")
    RolesRestClient rolesRestClient;

    WebMarkupContainer container;
    WebMarkupContainer membershipsContainer;
    AjaxButton submit;

    List<SchemaWrapper> schemaWrappers;
    List<MembershipTO> membershipTOs;
    final ModalWindow createUserWin;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public UserModalPage(final BasePage basePage, final ModalWindow window,
            final UserTO userTO, final boolean createFlag) {

        schemaWrappers = new ArrayList<SchemaWrapper>();

        add(createUserWin = new ModalWindow("membershipWin"));

        createUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
//        createUserWin.setInitialHeight(WIN_USER_HEIGHT);
//        createUserWin.setInitialWidth(WIN_USER_WIDTH);
        createUserWin.setPageMapName("create-membership-modal");
        createUserWin.setCookieName("create-membership-modal");

        Form userForm = new Form("UserForm");

        userForm.setModel(new CompoundPropertyModel(userTO));

        setupSchemaWrappers(createFlag, userTO);
        setupMemberships(createFlag,userTO);
        
        final ListView userAttributesView = new ListView("userSchemas", schemaWrappers) {

            @Override
            protected void populateItem(ListItem item) {
                final SchemaWrapper schemaWrapper = (SchemaWrapper) item.getDefaultModelObject();

                final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

                item.add(new Label("name", schemaWrapper.getSchemaTO().getName()));

                item.add(new ListView("fields", schemaWrapper.getValues()) {

                    Panel panel;

                    @Override
                    protected void populateItem(final ListItem item) {

                        if (schemaTO.getType().getClassName().equals("java.lang.String")) {
                            panel = new AjaxTextFieldPanel("panel", schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    item.setModelObject((String) object);
                                }
                            }, schemaTO.isMandatory());
                        } else if (schemaTO.getType().getClassName().equals("java.lang.Boolean")) {
                            panel = new AjaxCheckBoxPanel("panel", schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                    //return "false";
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    Boolean val = (Boolean) object;
                                    item.setModelObject(val.toString());
                                }
                            }, schemaTO.isMandatory());

                        } else if (schemaTO.getType().getClassName().equals("java.util.Date")) {
                            panel = new DateFieldPanel("panel", schemaTO.getName(),
                                    new Model() {

                                        @Override
                                        public Serializable getObject() {
                                            DateFormat formatter = new SimpleDateFormat(schemaTO.getConversionPattern());
                                            Date date = new Date();

                                            try {
                                                String dateValue = (String) item.getModelObject();
                                                formatter = new SimpleDateFormat("yyyy-MM-dd");

                                                if(!dateValue.equals(""))
                                                    date = formatter.parse((String) item.getModelObject());
                                                
                                            } catch (ParseException ex) {
                                                Logger.getLogger(UserModalPage.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            return date;
                                        }

                                        @Override
                                        public void setObject(Serializable object) {
                                            Date date = (Date) object;
                                            Format formatter = new SimpleDateFormat(schemaTO.getConversionPattern());
                                            String val = formatter.format(date);
                                            item.setModelObject(val);
                                        }
                                    }, schemaTO.isMandatory());
                        }

                        item.add(panel);
                    }
                });

                AjaxButton addButton = new AjaxButton("add", new Model(getString("add"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        schemaWrapper.getValues().add("");

                        target.addComponent(container);
                    }
                };

                AjaxButton dropButton = new AjaxButton("drop", new Model(getString("drop"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        //Drop the last component added
                        schemaWrapper.getValues().remove(schemaWrapper.getValues().size() - 1);

                        target.addComponent(container);
                    }
                };

                if (schemaTO.getType().getClassName().equals("java.lang.Boolean")) {
                    addButton.setVisible(false);
                    dropButton.setVisible(false);
                }

                addButton.setDefaultFormProcessing(false);
                addButton.setVisible(schemaTO.isMultivalue());

                dropButton.setDefaultFormProcessing(false);
                dropButton.setVisible(schemaTO.isMultivalue());

                if (schemaWrapper.getValues().size() == 1) {
                    dropButton.setVisible(false);
                }

                item.add(addButton);
                item.add(dropButton);
            }
        };

        userForm.add(userAttributesView);

        final ListMultipleChoiceTransfer resourcesTransfer =
                new ListMultipleChoiceTransfer("resourcesChoiceTransfer",
                getString("firstResourcesList"), getString("secondResourcesList")) {

                    @Override
                    public List<String> setupOriginals() {
                        //Originals:user's resources
                        List<String> resources = new ArrayList<String>();

                        for (String resourceName : userTO.getResources()) {
                            resources.add(resourceName);
                        }

                        return resources;

                    }

                    @Override
                    public List<String> setupDestinations() {
                        //Destinations:available resources
                        List<String> resources = new ArrayList<String>();

                        ResourcesRestClient resourcesRestClient = (ResourcesRestClient) ((SyncopeApplication) Application.get()).getApplicationContext().getBean("resourcesRestClient");

                        ResourceTOs resourcesTos = resourcesRestClient.getAllResources();

                        if (userTO.getResources().size() == 0) {
                            for (ResourceTO resourceTO : resourcesTos) {
                                resources.add(resourceTO.getName());
                            }

                        } else {

                            for (String resource : userTO.getResources()) {
                                for (ResourceTO resourceTO : resourcesTos) {
                                    if (!resource.equals(resourceTO.getName())) {
                                        resources.add(resourceTO.getName());
                                    }
                                }
                            }
                        }
                        return resources;
                    }
                };

        userForm.add(resourcesTransfer);

        container = new WebMarkupContainer("container");
        container.add(userAttributesView);

        PasswordTextField password = new PasswordTextField("password");
        password.setRequired(createFlag);
        password.setResetPassword(false);
        container.add(password);

        container.setOutputMarkupId(true);

        userForm.add(container);

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                UserTO userTO = (UserTO) form.getDefaultModelObject();

                boolean res = false;

                try {

                    userTO.setResources(getResourcesSet(resourcesTransfer.getFinalSelections()));
                    userTO.setAttributes(getUserAttributesSet());
                    userTO.setMemberships(getMembershipsSet());

                        if (createFlag) {
                            usersRestClient.createUser(userTO);
                        } else {
                            res = usersRestClient.updateUser(userTO);

                        if (!res) {
                            error(getString("error_updating"));
                        }

                    }

                    window.close(target);

                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        };

        userForm.add(submit);

        userForm.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        //Roles Tab
        SyncopeRoleTree roleTree = new SyncopeRoleTree(rolesRestClient);

        BaseTree tree;

        tree = new LinkTree("treeTable", roleTree.createTreeModel()) {

            @Override
            protected IModel<Object> getNodeTextModel(IModel<Object> model) {
                return new PropertyModel(model, "userObject.treeNode.name");
            }

            @Override
            protected void onNodeLinkClicked(final Object node, final BaseTree tree,
                    final AjaxRequestTarget target) {

                DefaultMutableTreeNode syncopeTreeNode = (DefaultMutableTreeNode) node;
                final TreeModelBean treeModel = (TreeModelBean) syncopeTreeNode.getUserObject();

                if (treeModel.getTreeNode() != null){

                createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                 MembershipTO membershipTO;
                 
                    @Override
                    public Page createPage() {

                            membershipTO = new MembershipTO();
                            membershipTO.setRole(treeModel.getTreeNode().getId());
                            String title = treeModel.getTreeNode().getName();

                            MembershipModalPage form =
                                    new MembershipModalPage(getPage(), createUserWin,
                                    membershipTO, true);

                            return form;
                    }
                });
                createUserWin.show(target);
                }
            }
        };

        tree.getTreeState().expandAll();
        tree.updateTree();

        userForm.add(tree);


        ListView membershipsView = new ListView("memberships", membershipTOs) {

            @Override
            protected void populateItem(final ListItem item) {
                final MembershipTO membershipTO =
                        (MembershipTO) item.getDefaultModelObject();

                item.add(new Label("roleId", new Model(membershipTO.getRole())));

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {

                            MembershipModalPage form = new MembershipModalPage(
                                getPage(), createUserWin,membershipTO, false);

                            return form;  

                            }
                        });
                        createUserWin.show(target);
                    }
                };
                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            int componentId = new Integer(getParent().getId());
                            membershipTOs.remove(componentId);
                            
                            target.addComponent(membershipsContainer);
                        }
                    };
                item.add(deleteLink);
            }
        };

        membershipsContainer = new WebMarkupContainer("membershipsContainer");
        membershipsContainer.add(membershipsView);
        membershipsContainer.setOutputMarkupId(true);

        setWindowClosedCallback(createUserWin, membershipsContainer);

        userForm.add(membershipsContainer);
        add(userForm);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                    }
                });
    }

    /**
     * Initialize the SchemaWrapper collection
     * @param create
     * @param userTO
     */

    public void setupSchemaWrappers(boolean create,UserTO userTO) {

        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        SchemaRestClient schemaRestClient = (SchemaRestClient)
                ((SyncopeApplication) Application.get())
                .getApplicationContext().getBean("schemaRestClient");

        SchemaTOs schemas = schemaRestClient.getAllUserSchemas();

        if (create) {
            for (SchemaTO schema : schemas) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            }
        } else {
            for (SchemaTO schema : schemas) {
                for (AttributeTO attribute : userTO.getAttributes()) {
                    if (schema.getName().equals(attribute.getSchema())) {
                        schemaWrapper = new SchemaWrapper(schema);
                        schemaWrapper.setValues(attribute.getValues());
                        schemaWrappers.add(schemaWrapper);
                    }
                }
            }
        }
    }

    /**
     * Initialize the membershipTOs
     * @param creation flag: true if a new User is being created, false otherwise
     * @param userTO object
     */
    public void setupMemberships(boolean create,UserTO userTO){
    
        membershipTOs = new ArrayList<MembershipTO>();
        
        if(!create) {
            Set<MembershipTO> memberships = userTO.getMemberships();

            for(MembershipTO membership : memberships)
            membershipTOs.add(membership);
          }
    }

    /**
     * Initialize the user's attributes
     * @param creation flag: true if a new User is being created, false otherwise
     * @param userTO object
     */
    public Set<AttributeTO> getUserAttributesSet() {

        Set<AttributeTO> attributes = new HashSet<AttributeTO>();

        AttributeTO attribute;

        for (SchemaWrapper schemaWrapper : schemaWrappers) {

            attribute = new AttributeTO();
            attribute.setSchema(schemaWrapper.getSchemaTO().getName());
            attribute.setValues(new HashSet<String>());

            for (String value : schemaWrapper.getValues()) {
                attribute.getValues().add(value);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    /**
     * Convert a memberships ArrayList in a memberships HashSet list.
     * @return Set<MembershipTO> selected for a new user.
     */
    public Set<MembershipTO> getMembershipsSet(){

        HashSet<MembershipTO> memberships = new HashSet<MembershipTO>();

        for (MembershipTO membership : membershipTOs) 
            memberships.add(membership);

        return memberships;
    }

    /**
     * Covert a resources List<String> to Set<String>.
     * @return Set<String>
     */
    public Set<String> getResourcesSet(List<String> resourcesList) {
        Set<String> resourcesSet = new HashSet<String>();

        for (String resource : resourcesList) {
            resourcesSet.add(resource);
        }

        return resourcesSet;
    }

    public List<MembershipTO> getMembershipTOs() {
        return membershipTOs;
    }

    public void setMembershipTOs(List<MembershipTO> membershipTOs) {
        this.membershipTOs = membershipTOs;
    }

    /**
     * Wrapper for User's Schema - Attribute.
     */
    public class SchemaWrapper {

        SchemaTO schemaTO;
        List<String> values;

        public SchemaWrapper(SchemaTO schemaTO) {
            this.schemaTO = schemaTO;
            values = new ArrayList<String>();

            if (schemaTO.getType().getClassName().equals("java.lang.Boolean")) {
                values.add("");//false
            } else {
                values.add("");
            }
        }

        public SchemaTO getSchemaTO() {
            return schemaTO;
        }

        public void setSchemaTO(SchemaTO schemaTO) {
            this.schemaTO = schemaTO;
        }

        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        public void setValues(Set<String> values) {
            this.values = new ArrayList<String>();
            for (String value : values) {
                this.values.add(value);
            }
        }
    }
}
