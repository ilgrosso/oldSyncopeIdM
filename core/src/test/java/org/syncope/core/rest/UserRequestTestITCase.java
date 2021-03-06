/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.types.SyncopeClientExceptionType;

public class UserRequestTestITCase extends AbstractTest {

    @Test
    public void selfRead() {
        PreemptiveAuthHttpRequestFactory requestFactory =
                ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).
                getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(),
                new UsernamePasswordCredentials("user1", "password"));

        SyncopeClientException exception = null;
        try {
            restTemplate.getForObject(
                    BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(
                    SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        UserTO userTO = restTemplate.getForObject(
                BASE_URL + "user/request/read/self", UserTO.class);
        assertEquals("user1", userTO.getUsername());
    }

    @Test
    public void create() {
        // 1. set create request not allowed
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("createRequest.allowed");
        configurationTO.setValue("false");

        configurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                configurationTO, ConfigurationTO.class);
        assertNotNull(configurationTO);

        UserTO userTO = UserTestITCase.getSampleTO(
                "selfcreate@syncope-idm.org");

        // 2. get unauthorized when trying to request user create
        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/create",
                    userTO, UserRequestTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(
                    SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. set create request allowed
        configurationTO.setValue("true");

        configurationTO = restTemplate.postForObject(
                BASE_URL + "configuration/create",
                configurationTO, ConfigurationTO.class);
        assertNotNull(configurationTO);

        // 4. as anonymous, request user create works
        UserRequestTO request = anonymousRestTemplate().postForObject(
                BASE_URL + "user/request/create", userTO, UserRequestTO.class);
        assertNotNull(request);

        // 5. switch back to admin
        super.setupRestTemplate();

        // 6. try to find user
        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.EQ);
        attrCond.setSchema("userId");
        attrCond.setExpression("selfcreate@syncope-idm.org");

        final List<UserTO> matchingUsers = Arrays.asList(
                restTemplate.postForObject(BASE_URL + "user/search",
                NodeCond.getLeafCond(attrCond), UserTO[].class));
        assertTrue(matchingUsers.isEmpty());

        // 7. actually create user
        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", request.getUserTO(), UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public void update() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getSampleTO(
                "selfupdate@syncope-idm.org");
        String initialPassword = userTO.getPassword();

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(initialPassword);

        // 2. try to request user update as admin: failure
        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/update",
                    userMod, UserRequestTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(
                    SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        PreemptiveAuthHttpRequestFactory requestFactory =
                ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).
                getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(),
                new UsernamePasswordCredentials(userTO.getUsername(),
                initialPassword));

        // 4. update with same password: not matching password policy
        exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/update",
                    userMod, UserRequestTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            exception = scce.getException(
                    SyncopeClientExceptionType.InvalidSyncopeUser);
        }
        assertNotNull(exception);

        // 5. now request user update works
        userMod.setPassword("new" + initialPassword);
        UserRequestTO request = restTemplate.postForObject(
                BASE_URL + "user/request/update",
                userMod, UserRequestTO.class);
        assertNotNull(request);

        // 6. switch back to admin
        super.setupRestTemplate();

        // 7. user password has not changed yet
        Boolean verify = restTemplate.getForObject(
                BASE_URL + "user/verifyPassword/{userId}?password="
                + userMod.getPassword(),
                Boolean.class, userTO.getId());
        assertFalse(verify);

        // 8. actually update user
        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);
        assertNotNull(userTO);

        // 9. user password has now changed
        verify = restTemplate.getForObject(
                BASE_URL + "user/verifyPassword/{userId}?password="
                + userMod.getPassword(),
                Boolean.class, userTO.getId());
        assertTrue(verify);
    }

    @Test
    public void delete() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getSampleTO(
                "selfdelete@syncope-idm.org");
        String initialPassword = userTO.getPassword();

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        // 2. try to request user delete as admin: failure
        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/request/delete/",
                    userTO.getId(), UserRequestTO.class);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(
                    SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        // 3. auth as user just created
        PreemptiveAuthHttpRequestFactory requestFactory =
                ((PreemptiveAuthHttpRequestFactory) restTemplate.
                getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).
                getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(),
                new UsernamePasswordCredentials(userTO.getUsername(),
                initialPassword));

        // 4. now request user delete works
        UserRequestTO request = restTemplate.postForObject(
                BASE_URL + "user/request/delete",
                userTO.getId(), UserRequestTO.class);
        assertNotNull(request);

        // 5. switch back to admin
        super.setupRestTemplate();

        // 6. user still exists
        UserTO actual = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());
        assertNotNull(actual);

        // 7. actually delete user
        restTemplate.getForObject(BASE_URL + "user/delete/{userId}",
                UserTO.class, userTO.getId());

        // 8. user does not exist any more
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                    UserTO.class, userTO.getId());
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}
