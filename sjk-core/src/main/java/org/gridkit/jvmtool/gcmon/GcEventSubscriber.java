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
            }
        }
        return ok;
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
