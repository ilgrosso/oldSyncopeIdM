
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
package org.syncope.core.rest.controller;

import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.spi.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.rest.data.UserDataBinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
import org.syncope.core.persistence.dao.WorkflowEntryDAO;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.WorkflowInitException;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private SyncopeConfigurationDAO syncopeConfigurationDAO;
    @Autowired
    private UserDataBinder userDataBinder;
    @Autowired
    private Workflow userWorkflow;
    @Autowired
    private WorkflowEntryDAO workflowEntryDAO;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate/{userId}")
    public UserTO activate(HttpServletResponse response,
            @PathVariable("userId") Long userId,
            @RequestParam("token") String token)
            throws IOException {

        SyncopeUser syncopeUser = syncopeUserDAO.find(userId);

        if (syncopeUser == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, syncopeUser);
        if (token != null) {
            inputs.put(Constants.TOKEN, token);
        }

        WorkflowDescriptor workflowDescriptor =
                userWorkflow.getWorkflowDescriptor(Constants.USER_WORKFLOW);

        int[] actions = userWorkflow.getAvailableActions(
                syncopeUser.getWorkflowEntryId(), inputs);
        Integer activateActionId = null;
        for (int i = 0; i < actions.length && activateActionId == null; i++) {
            if (Constants.ACTION_ACTIVATE.equals(
                    workflowDescriptor.getAction(actions[i]).getName())) {

                activateActionId = actions[i];
            }
        }
        if (activateActionId != null) {
            try {
                userWorkflow.doAction(syncopeUser.getWorkflowEntryId(),
                        activateActionId, inputs);
            } catch (WorkflowException e) {
                log.error("While performing activate", e);

                return throwWorkflowException(e, response);
            }

            syncopeUser = syncopeUserDAO.save(syncopeUser);
        } else {
            log.error("No action named '" + Constants.ACTION_ACTIVATE
                    + "' has been found among available actions");
        }

        return userDataBinder.getUserTO(syncopeUser);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(HttpServletResponse response,
            @RequestBody UserTO userTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("create called with parameter " + userTO);
        }

        WorkflowInitException wie = null;
        Long workflowId = null;
        try {
            workflowId = userWorkflow.initialize(Constants.USER_WORKFLOW, 0,
                    Collections.singletonMap(Constants.USER_TO, userTO));
        } catch (WorkflowInitException e) {
            log.error("During workflow initialization: " + e, e);
            wie = e;

            // Removing dirty workflow entry
            if (e.getWorkflowEntry() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing dirty workflow entry "
                            + e.getWorkflowEntry());
                }

                workflowEntryDAO.delete(e.getWorkflowEntry().getId());

                if (log.isDebugEnabled()) {
                    log.debug("Removed dirty workflow entry "
                            + e.getWorkflowEntry());
                }
            }
        } catch (WorkflowException e) {
            log.error("Unexpected workflow exception", e);

            return throwWorkflowException(e, response);
        }

        if (wie != null) {
            switch (wie.getExceptionOperation()) {
                case OVERWRITE:
                    return update(response, userTO);
                case REJECT:
                    SyncopeClientCompositeErrorException compositeException =
                            new SyncopeClientCompositeErrorException(
                            HttpStatus.BAD_REQUEST);
                    SyncopeClientException rejectedUserCreate =
                            new SyncopeClientException(
                            SyncopeClientExceptionType.RejectedUserCreate);
                    rejectedUserCreate.addElement(
                            String.valueOf(wie.getSyncopeUserId()));
                    compositeException.addException(rejectedUserCreate);

                    return throwCompositeException(compositeException,
                            response);
            }
        }

        SyncopeUser syncopeUser = null;
        try {
            syncopeUser = userDataBinder.createSyncopeUser(userTO);
        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + userTO, e);
            return throwCompositeException(e, response);
        }
        syncopeUser.setWorkflowEntryId(workflowId);
        syncopeUser.setCreationTime(new Date());
        syncopeUser.generateToken(
                Integer.parseInt(syncopeConfigurationDAO.find(
                "token.length").getConfValue()),
                Integer.parseInt(syncopeConfigurationDAO.find(
                "token.expireTime").getConfValue()));
        syncopeUser = syncopeUserDAO.save(syncopeUser);
        userTO = userDataBinder.getUserTO(syncopeUser);

        int[] availableWorkflowActions = userWorkflow.getAvailableActions(
                workflowId, null);
        Map<String, SyncopeUser> inputs =
                Collections.singletonMap(Constants.SYNCOPE_USER, syncopeUser);
        for (int availableWorkflowAction : availableWorkflowActions) {
            try {
                userWorkflow.doAction(workflowId, availableWorkflowAction,
                        inputs);
            } catch (WorkflowException e) {
                log.error("Unexpected workflow exception", e);

                return throwWorkflowException(e, response);
            }
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return userTO;
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            throwNotFoundException(String.valueOf(userId), response);
        } else {
            if (user.getWorkflowEntryId() != null) {
                workflowEntryDAO.delete(user.getWorkflowEntryId());
            }

            syncopeUserDAO.delete(userId);
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public UserTOs list(HttpServletRequest request) throws IOException {
        List<SyncopeUser> users = syncopeUserDAO.findAll();
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        UserTOs result = new UserTOs();
        result.setUsers(userTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    public UserTO read(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {
        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        return userDataBinder.getUserTO(user);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/passwordReset/{userId}")
    public ModelAndView getPasswordResetToken(
            @PathVariable("userId") Long userId,
            @RequestParam("passwordResetFormURL") String passwordResetFormURL,
            @RequestParam("gotoURL") String gotoURL)
            throws IOException {
        log.info("passwordReset (GET) called with parameters " + userId + ", "
                + passwordResetFormURL + ", " + gotoURL);

        String passwordResetToken = "token";
        ModelAndView mav = new ModelAndView();

        mav.addObject(passwordResetToken);

        return mav;
    }

    @RequestMapping(method = RequestMethod.PUT,
    value = "/passwordReset/{userId}")
    public void passwordReset(@PathVariable("userId") Long userId,
            @RequestParam("tokenId") String tokenId,
            @RequestParam("newPassword") String newPassword)
            throws IOException {
        log.info("passwordReset (POST) called with parameters " + userId + ", "
                + tokenId + ", " + newPassword);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    public UserTOs search(HttpServletResponse response,
            @RequestBody SearchParameters searchParameters)
            throws IOException {

        log.info("search called with parameter " + searchParameters);

        List<UserTO> userTOs = new ArrayList<UserTO>();
        UserTOs result = new UserTOs();

        result.setUsers(userTOs);

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/status/{userId}")
    public String getStatus(HttpServletResponse response,
            @PathVariable("userId") Long userId) throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        List<Step> currentSteps = userWorkflow.getCurrentSteps(
                user.getWorkflowEntryId());
        if (currentSteps == null || currentSteps.isEmpty()) {
            return null;
        }

        return currentSteps.iterator().next().getStatus();
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(HttpServletResponse response,
            @RequestBody UserTO userTO)
            throws IOException {

        log.info("update called with parameter " + userTO);

        return userTO;
    }
}