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
package org.syncope.identityconnectors.bundles.staticwebservice.wstarget.test;

import java.util.ArrayList;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:beans.xml"})
public class ProvisioningTestITCase {

    private static final Logger log =
            LoggerFactory.getLogger(ProvisioningTestITCase.class);

    final private String ENDPOINT_PREFIX =
            "http://localhost:8888/wstarget/services";

    final private String SERVICE =
            "/provisioning";

    @Autowired
    JaxWsProxyFactoryBean proxyFactory;

    Provisioning provisioning;

    @Before
    public void init() {
        assertNotNull(proxyFactory);

        proxyFactory.setAddress(ENDPOINT_PREFIX + SERVICE);

        proxyFactory.setServiceClass(Provisioning.class);

        provisioning = (Provisioning) proxyFactory.create();
    }

    @Test
    public void authenticate() {
        Throwable t = null;

        try {

            String uid = provisioning.authenticate(
                    "TESTUSER",
                    "password");

            assertEquals("TESTUSER", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void checkAlive() {

        Throwable t = null;

        try {

            provisioning.checkAlive();

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);

    }

    @Test
    public void schema() {

        Throwable t = null;

        try {

            provisioning.schema();

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void create() {

        Throwable t = null;

        try {
            WSAttributeValue uid = new WSAttributeValue();
            uid.setName("userId");
            uid.setValue("john.doe@gmail.com");
            uid.setKey(true);

            WSAttributeValue password = new WSAttributeValue();
            password.setName("password");
            password.setValue("password");
            password.setPassword(true);

            WSAttributeValue type = new WSAttributeValue();
            type.setName("type");
            type.setValue("person");

            WSAttributeValue name = new WSAttributeValue();
            name.setName("name");
            name.setValue("john");

            WSAttributeValue surname = new WSAttributeValue();
            surname.setName("surname");
            surname.setValue("doe");

            WSAttributeValue birthdate = new WSAttributeValue();
            birthdate.setName("birthdate");
            birthdate.setValue("01/01/1990");

            List<WSAttributeValue> attrs = new ArrayList<WSAttributeValue>();
            attrs.add(uid);
            attrs.add(password);
            attrs.add(type);
            attrs.add(name);
            attrs.add(surname);
            attrs.add(birthdate);

            String accountId = provisioning.create(attrs);

            assertNotNull(accountId);
            assertEquals(accountId, "john.doe@gmail.com");

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void update() {

        Throwable t = null;

        try {

            WSAttributeValue surname = new WSAttributeValue();
            surname.setName("surname");
            surname.setValue("verde");
            surname.setKey(true);

            WSAttributeValue name = new WSAttributeValue();
            name.setName("name");
            name.setValue("pino");


            List<WSAttributeValue> attrs = new ArrayList<WSAttributeValue>();
            attrs.add(surname);
            attrs.add(name);

            String uid = provisioning.update("test2", attrs);

            assertNotNull(uid);
            assertEquals("test2", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void delete() {

        Throwable t = null;


        try {

            provisioning.delete("test1");

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void query() {

        Throwable t = null;


        try {

            Operand op1 = new Operand(Operator.EQ, "name", "Pino");
            Operand op2 = new Operand(Operator.EQ, "surname", "Bianchi");
            Operand op3 = new Operand(Operator.EQ, "surname", "Rossi");

            Set<Operand> sop1 = new HashSet<Operand>();
            sop1.add(op1);
            sop1.add(op2);

            Set<Operand> sop2 = new HashSet<Operand>();
            sop2.add(op1);
            sop2.add(op3);

            Operand op4 = new Operand(Operator.AND, sop1);
            Operand op5 = new Operand(Operator.AND, sop2);

            Set<Operand> sop = new HashSet<Operand>();
            sop.add(op4);
            sop.add(op5);

            Operand query = new Operand(Operator.OR, sop, true);

            List<WSUser> results = provisioning.query(query);

            assertNotNull(results);
            assertFalse(results.isEmpty());

            for (WSUser user : results) {
                log.debug("User: " + user);
            }


        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void resolve() {

        Throwable t = null;

        try {

            String uid = provisioning.resolve("test2");

            assertEquals("test2", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void getLatestChangeNumber() {

        Throwable t = null;

        try {

            int token = provisioning.getLatestChangeNumber();

            assertEquals(0, token);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void sync() {

        Throwable t = null;

        try {

            List<WSChange> results = null;

            if (provisioning.isSyncSupported()) {

                results = provisioning.sync();
                assertNotNull(results);

                for (WSChange change : results)
                    log.debug("Delta: " + change.getId());

            }

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }
}
