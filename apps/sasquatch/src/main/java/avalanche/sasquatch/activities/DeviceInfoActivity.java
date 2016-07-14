package avalanche.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import avalanche.core.ingestion.models.Device;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.NetworkStateHelper;
import avalanche.sasquatch.R;

public class DeviceInfoActivity extends AppCompatActivity implements NetworkStateHelper.Listener {
    private static final String[] METHOD_BLACK_LIST = {"getClass", "getToffset", "getType"};

    private NetworkStateHelper mNetworkStateHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        mNetworkStateHelper = NetworkStateHelper.getSharedInstance(this);
        mNetworkStateHelper.addListener(this);
        onNetworkStateUpdated(mNetworkStateHelper.isNetworkConnected());

        Device log;
        try {
            log = DeviceInfoHelper.getDeviceInfo(getApplicationContext());
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            Toast.makeText(getBaseContext(), R.string.error_device_info, Toast.LENGTH_LONG).show();
            return;
        }

        final List<DeviceInfoDisplayModel> list = getDeviceInfoDisplayModelList(log);

        ArrayAdapter<DeviceInfoDisplayModel> adapter = new ArrayAdapter<DeviceInfoDisplayModel>(this, android.R.layout.simple_list_item_2, android.R.id.text1, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(list.get(position).title);
                text2.setText(list.get(position).value);
                return view;
            }
        };

        ((ListView) findViewById(R.id.device_info_list_view)).setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        mNetworkStateHelper.removeListener(this);
        super.onDestroy();
    }

    private List<DeviceInfoDisplayModel> getDeviceInfoDisplayModelList(Device log) {
        List<DeviceInfoDisplayModel> list = new ArrayList<>();

        Method[] methods = Device.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && !isInBlackList(name)) {
                DeviceInfoDisplayModel model = new DeviceInfoDisplayModel();
                model.title = name.replace("get", "");
                try {
                    model.value = method.invoke(log).toString();
                } catch (Exception e) {
                    model.value = "N/A";
                }
                list.add(model);
            }
        }

        return list;
    }

    private boolean isInBlackList(String name) {
        for (String method : METHOD_BLACK_LIST) {
            if (method.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNetworkStateUpdated(boolean connected) {
        String message = "Network " + (connected ? "up" : "down");
        AvalancheLog.verbose(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class DeviceInfoDisplayModel {
        private String title;
        private String value;
    }
}