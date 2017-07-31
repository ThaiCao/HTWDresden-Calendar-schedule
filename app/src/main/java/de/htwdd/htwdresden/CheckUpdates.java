package de.htwdd.htwdresden;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import de.htwdd.htwdresden.classes.Const;
import de.htwdd.htwdresden.classes.MensaHelper;
import de.htwdd.htwdresden.classes.QueueCount;
import de.htwdd.htwdresden.classes.SemesterHelper;
import de.htwdd.htwdresden.classes.internet.VolleyDownloader;
import de.htwdd.htwdresden.types.Meal;
import de.htwdd.htwdresden.types.semsterPlan.Semester;
import de.htwdd.htwdresden.types.semsterPlan.TimePeriod;
import de.htwdd.htwdresden.types.studyGroups.StudyCourse;
import de.htwdd.htwdresden.types.studyGroups.StudyGroup;
import de.htwdd.htwdresden.types.studyGroups.StudyYear;
import io.realm.Realm;

/**
 *
 */
class CheckUpdates implements Runnable {
    private final static String LOG_TAG = "CheckUpdateTask";
    private final Context context;
    private final QueueCount queueCount;

    CheckUpdates(@NonNull final Context context) {
        this.context = context;
        queueCount = new QueueCount();
    }

    @Override
    public void run() {
        // Überprüfe Internetverbindung
        if (!VolleyDownloader.CheckInternet(context)) {
            return;
        }

        // Einstellungen holen
        final Realm realm = Realm.getDefaultInstance();
        final Calendar calendar = GregorianCalendar.getInstance();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final long mensaLastUpdate = sharedPreferences.getLong(Const.preferencesKey.PREFERENCES_MENSA_WEEK_LASTUPDATE, 0);
        final long studyGroupsLastUpdate = sharedPreferences.getLong(Const.preferencesKey.PREFERENCES_STUDY_GROUP_LAST_UPDATE, 0);

        // Aktualisiere Mensa
        if ((calendar.getTimeInMillis() - mensaLastUpdate) > TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS) || realm.where(Meal.class).count() == 0) {
            Log.d(LOG_TAG, "Lade Mensa");
            final MensaHelper mensaHelper = new MensaHelper(context, queueCount, (short) 9);
            mensaHelper.loadAndSaveMeals(1);
            queueCount.incrementCountQueue();
            mensaHelper.loadAndSaveMeals(2);
            queueCount.incrementCountQueue();
            queueCount.update = true;
        }

        // Überprüfe Version
        checkForUpdates();

        // Aktualisiere Studiengruppen
        if ((calendar.getTimeInMillis() - studyGroupsLastUpdate) > TimeUnit.MILLISECONDS.convert(14, TimeUnit.DAYS) || realm.where(StudyYear.class).count() == 0) {
            updateStudyGroups();
        }

        // Aktualisiere Semesterplan
        if (realm.where(Semester.class).count() == 0) {
            updateSemesterplan();
        }
        realm.close();

        // Wenn alle Mensa-Request abgeschlossen, Updatezeitpunkt speichern. Maximal 2 Minuten warten
        if (queueCount.update) {
            int count = 0;
            // Warte auf Mensa
            while (queueCount.getCountQueue() > 0 && count <= 240) {
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    Log.e(LOG_TAG, "Fehler beim versuch zu schlafen", e);
                }
                count++;
            }

            // Update-Datum nur bei erfolgreichen Download speichern
            if (queueCount.getCountQueue() == 0) {
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(Const.preferencesKey.PREFERENCES_MENSA_WEEK_LASTUPDATE, calendar.getTimeInMillis());
                editor.apply();
            }
        }
    }

    /**
     * Überprüfe Modul-Informationen
     */
    private void checkForUpdates() {
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                "https://www2.htw-dresden.de/~app/API/version.json",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                            final SharedPreferences.Editor editor = sharedPreferences.edit();
                            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

                            // Überprüfe APK-Version
                            editor.putBoolean("appUpdate", response.getInt("androidAPK") > packageInfo.versionCode);

                            // Überprüfe Semesterplan
                            if (response.optLong("semesterplan_update", 0) > sharedPreferences.getLong(Const.preferencesKey.PREFERENCES_SEMESTERPLAN_UPDATETIME, -1))
                                updateSemesterplan();

                            editor.putLong("appUpdateCheck", GregorianCalendar.getInstance().getTimeInMillis());
                            editor.apply();
                        } catch (PackageManager.NameNotFoundException | JSONException e) {
                            Log.e(LOG_TAG, "[Fehler] beim Überprüfen der App-Version: Daten: " + response);
                            Log.e(LOG_TAG, e.toString());
                        }
                    }
                },
                null);
        VolleyDownloader.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Aktualisiert den Semesterplan
     */
    private void updateSemesterplan() {
        final JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Const.internet.WEBSERVICE_URL_SEMESTERPLAN,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(final JSONArray response) {
                        try {
                            final JSONArray semesterPlan = SemesterHelper.convertSemesterPlanJsonObject(response);
                            final Realm realm = Realm.getDefaultInstance();
                            realm.beginTransaction();
                            realm.delete(TimePeriod.class);
                            realm.delete(Semester.class);
                            realm.createAllFromJson(Semester.class, semesterPlan);
                            realm.commitTransaction();
                            realm.close();

                            PreferenceManager.getDefaultSharedPreferences(context)
                                    .edit()
                                    .putLong(Const.preferencesKey.PREFERENCES_SEMESTERPLAN_UPDATETIME, Calendar.getInstance().getTimeInMillis())
                                    .apply();
                        } catch (final ParseException | JSONException e) {
                            Log.e(LOG_TAG, "[Fehler] Beim Verarbeiten der Studiengruppen", e);
                        }
                    }
                },
                null);
        VolleyDownloader.getInstance(context).addToRequestQueue(jsonArrayRequest);
    }

    /**
     * Studiengruppen aktualisieren
     */
    private void updateStudyGroups() {
        VolleyDownloader.getInstance(context).addToRequestQueue(
                new JsonArrayRequest(
                        Const.internet.WEBSERVICE_URL_STUDYGROUPS,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(final JSONArray response) {
                                final Realm realm = Realm.getDefaultInstance();
                                realm.beginTransaction();
                                realm.delete(StudyGroup.class);
                                realm.delete(StudyCourse.class);
                                realm.delete(StudyYear.class);
                                realm.createAllFromJson(StudyYear.class, response);
                                realm.commitTransaction();
                                realm.close();

                                PreferenceManager.getDefaultSharedPreferences(context)
                                        .edit()
                                        .putLong(Const.preferencesKey.PREFERENCES_MENSA_WEEK_LASTUPDATE, Calendar.getInstance().getTimeInMillis())
                                        .apply();
                            }
                        },
                        null)
        );
    }

}
