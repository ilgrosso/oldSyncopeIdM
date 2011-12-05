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

import org.junit.Test;

public class ResourceTestITCase extends AbstractTest {

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseCreateModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/div/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//form/div[2]/div/div/div/div/label[text()='Name']")) {
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
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//td[4]/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//form/div[2]/div/div/div/div/label[text()='Name']")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//li[2]/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent("//tbody/tr")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        selenium.click("//tbody/tr[2]/td/input");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
        selenium.click("name=apply");
    }

    @Test
    public void delete() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//tr[3]/td[5]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }

    @Test
    @SuppressWarnings("SleepWhileHoldingLock")
    public void checkSecurityTab() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Resources\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//td[4]/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//form/div[2]/div/div/div/div/label[text()='Name']")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//li[4]/a");

        assertTrue(selenium.isElementPresent("//label[@for='passwordPolicy']"));

        selenium.click("//li[1]/a");
        selenium.click("//li[2]/a");
        selenium.click("//li[3]/a");
        
        selenium.click("css=a.w_close");
    }
}
