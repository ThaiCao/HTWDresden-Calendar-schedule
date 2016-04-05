package de.htwdd.htwdresden.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

import de.htwdd.htwdresden.MainActivity;
import de.htwdd.htwdresden.R;
import de.htwdd.htwdresden.TimetableWidget;
import de.htwdd.htwdresden.classes.Const;
import de.htwdd.htwdresden.classes.LessonHelper;
import de.htwdd.htwdresden.types.Lesson;
import de.htwdd.htwdresden.types.LessonSearchResult;

/**
 * Service zum regelmäßigen Updaten des Stundenplan Widgets
 *
 * @author Kay Förster
 */
public class TimetableWidgetService extends Service {

    private void updateWidget() {
        final RemoteViews view = new RemoteViews(getPackageName(), R.layout.widget_timetable);
        final Context context = getApplicationContext();
        final String[] lessonType = getResources().getStringArray(R.array.lesson_type);

        // Aktuelle Stunde anzeigen
        LessonSearchResult lessonSearchResult = LessonHelper.getCurrentUserLesson(context);
        Lesson lesson;
        switch (lessonSearchResult.getCode()) {
            case Const.Timetable.NO_LESSON_FOUND:
                view.setTextViewText(R.id.widget_timetable_lesson_time, null);
                view.setTextViewText(R.id.widget_timetable_lesson, null);
                view.setTextViewText(R.id.widget_timetable_room, null);
                break;
            case Const.Timetable.ONE_LESSON_FOUND:
                lesson = lessonSearchResult.getLesson();
                assert lesson != null;

                view.setTextViewText(R.id.widget_timetable_lesson_time, lessonSearchResult.getTimeRemaining());
                view.setTextViewText(R.id.widget_timetable_lesson, lesson.getName());
                if (!lesson.getRooms().isEmpty())
                    view.setTextViewText(R.id.widget_timetable_room, getString(
                            R.string.timetable_ds_list_simple,
                            lessonType[lesson.getTypeInt()],
                            lesson.getRooms()
                    ));
                else
                    view.setTextViewText(R.id.widget_timetable_room, lessonType[lesson.getTypeInt()]);
                break;
            case Const.Timetable.MORE_LESSON_FOUND:
                view.setTextViewText(R.id.widget_timetable_lesson_time, lessonSearchResult.getTimeRemaining());
                view.setTextViewText(R.id.widget_timetable_lesson, getString(R.string.timetable_moreLessons));
                view.setTextViewText(R.id.widget_timetable_room, null);
                break;
        }

        // Nächste Stunde anzeigen
        lessonSearchResult = LessonHelper.getNextUserLesson(context);
        switch (lessonSearchResult.getCode()) {
            case Const.Timetable.NO_LESSON_FOUND:
                view.setTextViewText(R.id.widget_timetable_lesson_time_next, null);
                view.setTextViewText(R.id.widget_timetable_lesson_next, null);
                view.setTextViewText(R.id.widget_timetable_room_next, null);
                break;
            case Const.Timetable.ONE_LESSON_FOUND:
                lesson = lessonSearchResult.getLesson();
                assert lesson != null;
                view.setTextViewText(R.id.widget_timetable_lesson_time_next, lessonSearchResult.getTimeRemaining());
                view.setTextViewText(R.id.widget_timetable_lesson_next, lesson.getName());
                if (!lesson.getRooms().isEmpty())
                    view.setTextViewText(R.id.widget_timetable_room_next, getString(
                            R.string.timetable_ds_list_simple,
                            lessonType[lesson.getTypeInt()],
                            lesson.getRooms()
                    ));
                else
                    view.setTextViewText(R.id.widget_timetable_room_next, lessonType[lesson.getTypeInt()]);
                break;
            case Const.Timetable.MORE_LESSON_FOUND:
                view.setTextViewText(R.id.widget_timetable_lesson_time_next, lessonSearchResult.getTimeRemaining());
                view.setTextViewText(R.id.widget_timetable_lesson_next, getString(R.string.timetable_moreLessons));
                view.setTextViewText(R.id.widget_timetable_room_next, null);
                break;
        }

        // Erstelle Intent zum Starten der App
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Const.IntentParams.START_ACTION_TIMETABLE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, PendingIntent.FLAG_UPDATE_CURRENT, intent, 0);
        view.setOnClickPendingIntent(R.id.timetable_widget_layout, pendingIntent);

        // Update das Widget
        ComponentName thisWidget = new ComponentName(this, TimetableWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, view);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWidget();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}