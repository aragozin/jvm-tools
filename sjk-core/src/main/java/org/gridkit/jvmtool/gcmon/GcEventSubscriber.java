/**
 * Copyright 2017 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.gcmon;

import java.io.IOException;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class GcEventSubscriber extends GcEventPoller {

    private static Listener LISTENER = new Listener();

    public GcEventSubscriber(MBeanServerConnection mserver, GarbageCollectionEventConsumer eventSink) {
        super(mserver, eventSink);
    }

    public boolean subscribe() {
        boolean ok = true;
        for(GcTracker tracker: trackers) {
            try {
                subscribeTracker(tracker);
            }
            catch(Exception e) {
                ok = false;
                unsubscribe();
                break;
            }
        }
        return ok;
    }
    
    public void unsubscribe() {
        for(GcTracker tracker: trackers) {
            try {
                mserver.removeNotificationListener(tracker.name, LISTENER, null, tracker);
            }
            catch(Exception e) {
            }
        }      
    }

    protected void subscribeTracker(GcTracker tracker) throws IOException, JMException {
        mserver.addNotificationListener(tracker.name, LISTENER, null, tracker);
        tracker.capture(); // resync poll/push
    }

    private static class Listener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            try {
                GcTracker tracker = (GcTracker) handback;
                CompositeData cdata = (CompositeData) notification.getUserData();
                CompositeData gcInfo = (CompositeData) cdata.get("gcInfo");

                tracker.processGcEvent(gcInfo);
            } catch (JMException e) {
                // ignore
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
