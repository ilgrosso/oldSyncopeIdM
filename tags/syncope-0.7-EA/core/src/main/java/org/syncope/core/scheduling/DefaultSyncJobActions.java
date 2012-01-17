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
package org.syncope.core.scheduling;

import java.util.List;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;

/**
 * Default (empty) implementation of SyncJobActions.
 * @see SyncJobActions
 */
public class DefaultSyncJobActions implements SyncJobActions {

    @Override
    public void beforeAll(final List<SyncDelta> deltas)
            throws JobExecutionException {
    }

    @Override
    public void beforeCreate(final SyncDelta delta, final UserTO user)
            throws JobExecutionException {
    }

    @Override
    public void beforeUpdate(SyncDelta delta, UserTO user, UserMod userMod)
            throws JobExecutionException {
    }

    @Override
    public void beforeDelete(SyncDelta delta, UserTO user)
            throws JobExecutionException {
    }

    @Override
    public void after(final SyncDelta delta, final UserTO user,
            final SyncResult result)
            throws JobExecutionException {
    }

    @Override
    public void afterAll(final List<SyncDelta> deltas,
            final List<SyncResult> results)
            throws JobExecutionException {
    }
}
