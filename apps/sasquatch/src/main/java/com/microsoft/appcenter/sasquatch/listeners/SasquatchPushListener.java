package com.microsoft.appcenter.sasquatch.listeners;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.appcenter.push.PushNotification;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.activities.MainActivity;

import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class SasquatchPushListener implements com.microsoft.appcenter.push.PushListener {

    @Override
    public void onPushNotificationReceived(Activity activity, PushNotification pushNotification) {

//        // Set up an intent so that tapping the notifications returns to this app:
//        //Intent intent = new Intent(this, MainActivity.class);
//        Intent intent = new Intent();
//        intent.putExtra("Notification_Id", 1001);
//
//        // Create a PendingIntent; we're only using one PendingIntent (ID = 0):
//        int pendingIntentId = 0;
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(this, pendingIntentId, intent, PendingIntent.FLAG_ONE_SHOT);
//
//        // Build the notification:
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
//                .setAutoCancel(true) // Dismiss the notification from the notification area when the user clicks on it
//                .setContentIntent(pendingIntent) // Start up this activity when the user clicks the intent.
//                .setDefaults(1)
//                .setContentTitle("eventTitle") // Set the title
//                //.setSmallIcon(Resource.Drawable.icon) // This is the icon to display
//                .setContentText("Message blah"); // the message to display.
//
//        // Finally, publish the notification:
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        notificationManager.notify(123, builder.build());


        String title = pushNotification.getTitle();
        String message = pushNotification.getMessage();
        Map<String, String> customData = pushNotification.getCustomData();
        Log.i(LOG_TAG, "Push received title=" + title + " message=" + message + " customData=" + customData + " activity=" + activity);
        if (message != null) {
            android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(activity);
            dialog.setTitle(title);
            dialog.setMessage(message);
            if (!customData.isEmpty()) {
                dialog.setMessage(message + "\n" + customData);
            }
            dialog.setPositiveButton(android.R.string.ok, null);
            dialog.show();
        } else {
            Toast.makeText(activity, String.format(activity.getString(R.string.push_toast), customData), Toast.LENGTH_LONG).show();
        }
    }
}
