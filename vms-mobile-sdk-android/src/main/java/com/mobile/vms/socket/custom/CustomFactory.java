package com.mobile.vms.socket.custom;

import com.mobile.vms.socket.custom.CustomWebSocketClientWrapper;
import com.mobile.vms.socket.custom.CustomWebSocketConnection;
import com.pusher.client.Authorizer;
import com.pusher.client.PusherOptions;
import com.pusher.client.connection.impl.InternalConnection;
import com.pusher.client.connection.websocket.WebSocketListener;

import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLException;

class CustomFactory {

    private static final Object eventLock = new Object();
    private InternalConnection connection;
    private CustomChannelManager channelManager;
    private ExecutorService eventQueue;
    private ScheduledExecutorService timers;

    public synchronized InternalConnection getConnection(final String apiKey, final PusherOptions options) {
        if (connection == null) {
            connection = new CustomWebSocketConnection(
                    options.buildUrl(apiKey),
                    options.getActivityTimeout(),
                    options.getPongTimeout(),
                    options.getMaxReconnectionAttempts(),
                    options.getMaxReconnectGapInSeconds(),
                    options.getProxy(),
                    this);
        }
        return connection;
    }

    public CustomWebSocketClientWrapper newWebSocketClientWrapper(final URI uri, final Proxy proxy, final WebSocketListener webSocketListener) throws SSLException {
        return new CustomWebSocketClientWrapper(uri, proxy, webSocketListener);
    }

    public synchronized ScheduledExecutorService getTimers() {
        if (timers == null) {
            timers = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("timers"));
        }
        return timers;
    }

    public CustomChannelImpl newPublicChannel(final String channelName) {
        return new CustomChannelImpl(channelName, this);
    }

    public CustomPrivateChannelImpl newPrivateChannel(final InternalConnection connection, final String channelName,
                                                      final Authorizer authorizer) {
        return new CustomPrivateChannelImpl(connection, channelName, authorizer, this);
    }

    public CustomPresenceChannelImpl newPresenceChannel(final InternalConnection connection, final String channelName,
                                                        final Authorizer authorizer) {
        return new CustomPresenceChannelImpl(connection, channelName, authorizer, this);
    }

    public synchronized CustomChannelManager getChannelManager() {
        if (channelManager == null) {
            channelManager = new CustomChannelManager(this);
        }
        return channelManager;
    }

    public synchronized void queueOnEventThread(final Runnable r) {
        if (eventQueue == null) {
            eventQueue = Executors.newSingleThreadExecutor(new DaemonThreadFactory("eventQueue"));
        }
        eventQueue.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (eventLock) {
                    r.run();
                }
            }
        });
    }

    public synchronized void shutdownThreads() {
        if (eventQueue != null) {
            eventQueue.shutdown();
            eventQueue = null;
        }
        if (timers != null) {
            timers.shutdown();
            timers = null;
        }
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final String name;

        public DaemonThreadFactory(final String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("pusher-java-client " + name);
            return t;
        }
    }
}
