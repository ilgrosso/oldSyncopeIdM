/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console;

import org.junit.Before;
import org.junit.Test;

public class EditProfileTestITCase extends AbstractTest {

    @Override
    @Before
    public void setUp()
            throws Exception {

        super.setUp(BASE_URL, "*firefox");
    }

    @Test
    public void selfRegistration() {
        selenium.setSpeed("1000");

        selenium.open("/syncope-console/");

        selenium.click("//div/span/span/a");

        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("css=a.w_close");

        // only to have some "Logout" availabe for @After
        selenium.type("name=userId", "user1");
        selenium.type("name=password", "password");
        selenium.click("name=:submit");
        selenium.waitForPageToLoad("30000");
    }

    @Test
    public void editUserProfile() {
        selenium.setSpeed("1000");

        selenium.open("/syncope-console/");
        selenium.type("name=userId", "user1");
        selenium.type("name=password", "password");
        selenium.click("name=:submit");
        selenium.waitForPageToLoad("30000");

        selenium.click("css=img[alt=\"Users\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("id=username");
        selenium.click("//span[@id='editProfile']/a");

        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        assertTrue(selenium.isElementPresent("//input[@value='user1']"));

        selenium.click("css=a.w_close");
    }
}