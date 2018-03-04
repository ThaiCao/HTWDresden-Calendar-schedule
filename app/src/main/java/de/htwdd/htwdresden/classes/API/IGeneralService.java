package de.htwdd.htwdresden.classes.API;

import java.util.List;

import de.htwdd.htwdresden.types.News;
import de.htwdd.htwdresden.types.semsterPlan.Semester;
import de.htwdd.htwdresden.types.studyGroups.StudyYear;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * API zum Abruf des allgemeiner Informationen
 *
 * @author Kay Förster
 */
public interface IGeneralService {
    @GET("v0/semesterplan.json")
    Call<List<Semester>> getSemesterplan();

    @GET("v0/studyGroups.php")
    Call<List<StudyYear>> getStudyGroups();

    @GET("v0/news.json")
    Call<List<News>> getNews();
}
