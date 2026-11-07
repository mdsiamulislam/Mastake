
package me.binarylife.mastaka.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import me.binarylife.mastaka.R;
import me.binarylife.mastaka.activities.NoteCreateActivity;

public class MyWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_layout
            );

            // Open Note Create Activity
            Intent intent = new Intent(context, NoteCreateActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
            );

            // Whole widget click
            views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    pendingIntent
            );

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
