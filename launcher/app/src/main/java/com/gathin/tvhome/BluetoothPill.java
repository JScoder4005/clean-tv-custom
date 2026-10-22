package com.gathin.tvhome;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which bonded Bluetooth devices are connected, for the Bluetooth pill. A cold start asks the three
 * profiles a TV remote, headset or speaker binds to (HID_DEVICE, A2DP, HEADSET) what is connected right now,
 * then the ACL connect/disconnect broadcasts every profile sends keep that current, so nothing needs to stay
 * bound to a profile proxy. No reflection or hidden API: everything here is public.
 */
final class BluetoothPill {

    private final Context context;
    private final BluetoothAdapter adapter;
    private final Runnable onChanged;
    private final Set<BluetoothDevice> connected = new LinkedHashSet<>();
    private boolean registered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null) connected.add(d);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null) connected.remove(d);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && !adapter.isEnabled()) {
                connected.clear();
            }
            onChanged.run();
        }
    };

    BluetoothPill(Context context, Runnable onChanged) {
        this.context = context;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.onChanged = onChanged;
    }

    /** No Bluetooth radio on this set: the pill stays hidden for good, start() is never called. */
    boolean available() {
        return adapter != null;
    }

    boolean enabled() {
        return adapter != null && adapter.isEnabled();
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    /** Registers the ACL receiver and asks HID_DEVICE, A2DP and HEADSET for a one-shot snapshot: the
     *  broadcasts above only fire on the next change, not for a connection already up when Home starts. */
    void start() {
        if (adapter == null || registered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        registered = true;
        if (!hasConnectPermission()) return;
        for (int profile : new int[]{BluetoothProfile.HID_DEVICE, BluetoothProfile.A2DP, BluetoothProfile.HEADSET}) {
            adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int p, BluetoothProfile proxy) {
                    try {
                        connected.addAll(proxy.getConnectedDevices());
                    } catch (SecurityException ignored) {
                    }
                    adapter.closeProfileProxy(p, proxy);
                    onChanged.run();
                }

                @Override
                public void onServiceDisconnected(int p) {
                }
            }, profile);
        }
    }

    void stop() {
        if (!registered) return;
        context.unregisterReceiver(receiver);
        registered = false;
    }

    /** Names of the devices currently connected, best-effort: empty before BLUETOOTH_CONNECT is granted. */
    List<String> connectedNames() {
        List<String> names = new ArrayList<>();
        if (!hasConnectPermission()) return names;
        for (BluetoothDevice d : connected) {
            try {
                String name = d.getName();
                names.add(name != null ? name : d.getAddress());
            } catch (SecurityException ignored) {
            }
        }
        return names;
    }
}
