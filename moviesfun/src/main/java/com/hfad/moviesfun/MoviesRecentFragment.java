package com.hfad.moviesfun;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.hfad.moviesfun.Utilities.MovieMultipleJSONArray;
/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the
 * interface.
 */
public class MoviesRecentFragment extends Fragment {

    public static final String ITEMS_ARRAY_NAME = "results";
    private ListView movieListView;
    private View loadMore;
    private View fragmentView;

    private OkHttpClient client = new OkHttpClient();
    private MoviesAdapter adapter;
    private ArrayList<MovieDataModel> discover_moviesArrayList;

    private MoviesAdapter moviesAdapter;
    private Context mContext;
    private int page=1;
    private String discover_url;

    ArrayList<MovieDataModel> display2 = new ArrayList<>();

    private  String TAG = getClass().getName();
    private Utilities utils;
    OnListItemClickCallback mCallback;
    Activity hostActivity;

    String discover_responseString;

    public MoviesRecentFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        hostActivity = (MainActivity) activity;
        try{
            mCallback = (OnListItemClickCallback) activity;
        }catch(ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement OnListItemClickCallback");
        }
        //((MainActivity) activity).setCurrentFragment(MainActivity.fragmentTags.MAIN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "In onCreateView()");
        mContext = getActivity().getBaseContext();
        utils = new Utilities(mContext);

        String fragmentTag = getTag();
        if(fragmentTag!=null){
            mCallback.updateActivityUI(fragmentTag);
        }else{
            Log.e(TAG, "fragment was null");
        }

        fragmentView =  inflater.inflate(R.layout.fragment_list, null);
        movieListView = (ListView)fragmentView.findViewById(R.id.moviesListView);

        loadMore = inflater.inflate(R.layout.load_more,null);
        loadMore.setOnClickListener(new LoadMoreOnClickListener());
        MySingleton singleton = MySingleton.getInstance();
        ArrayList<MovieDataModel> savedList = singleton.getData();
        ArrayList<MovieDataModel> display1 = new ArrayList<>();
        if(savedList==null){


            try {
                discover_url = utils.getRecentReleasedMoviesUrl();
                discover_responseString = new ServiceResponseAsyncTask(client).execute(discover_url).get();
                Log.d(TAG, "Got response from network");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            discover_moviesArrayList = (ArrayList<MovieDataModel>) getListFromNetworkResponse(discover_responseString);
            Log.d(TAG, "Got response from getListFromNetworkResponse()");
            //Sort by date

            Collections.sort(discover_moviesArrayList, new Comparator<MovieDataModel>() {
                @Override
                public int compare(MovieDataModel lhs, MovieDataModel rhs) {
                    if (lhs.releaseDate == null || rhs.releaseDate == null) return 0;
                    return rhs.releaseDate.compareTo(lhs.releaseDate);
                }
            });


            movieListView.addFooterView(loadMore);


            for(int i=0; i<10; i++) display1.add(discover_moviesArrayList.get(i));

            display1 = addCreditsToList(display1);

            adapter = new MoviesAdapter(mContext,display1);
            movieListView.setAdapter(adapter);


        }else{

            Log.d(TAG, "Using saved list");
            discover_moviesArrayList = savedList;

            if(singleton.getLoadMore()){
                Log.d(TAG, "Loaded more data");
                adapter = new MoviesAdapter(mContext,discover_moviesArrayList);
            }else{
                for(int i=0; i<10; i++) display1.add(discover_moviesArrayList.get(i));
                display1 = addCreditsToList(display1);
                movieListView.addFooterView(loadMore);
                adapter = new MoviesAdapter(mContext,display1);
            }

            movieListView.setAdapter(adapter);
        }





        movieListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCallback.onListItemClick(id,
                        discover_moviesArrayList.get(position).backdropPath,
                        discover_moviesArrayList.get(position).posterPath,
                        MainActivity.fragmentTags.MAIN);
            }
        });

        return fragmentView;
    }


    protected ArrayList<MovieDataModel> addCreditsToList(ArrayList<MovieDataModel> list){

        String credits;
        for(int k=0; k< list.size()   ;k++){

            MovieDataModel currentMovieObj = list.get(k);
            String creditRequestString = utils.getCreditsUrl(currentMovieObj.id);

            try {

                //credits for each movie.
                credits = new ServiceResponseAsyncTask(client).execute(creditRequestString).get();
                ArrayList<String> actors = new ArrayList<>();
                JSONArray castJsonArray =  new JSONObject(credits).getJSONArray(Utilities.CAST_ARRAY_NAME);

                for(int i=0; i<castJsonArray.length();i++){
                    String cast = castJsonArray.getJSONObject(i).getString(Utilities.ACTOR_STRING_NAME);
                    actors.add(cast);
                }

                list.get(k).actors = actors;
            } catch (JSONException e) {
                e.printStackTrace();
            }  catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }


        }
        return list;
    }

    protected List<MovieDataModel> getListFromNetworkResponse(String networkResponse){

        List<MovieDataModel> moviesList = new ArrayList<>();

        try {


            JSONObject movieObj = new JSONObject(networkResponse);

            JSONArray resultsArray = movieObj.getJSONArray(ITEMS_ARRAY_NAME);

            //for each movie
            for(int i=0; i<resultsArray.length();i++){



                movieObj =resultsArray.getJSONObject(i);

                int[] genres = getGenreArray(movieObj);

                Long id = movieObj.getLong(Utilities.MovieMultipleJSONArray.ID);
                String title = movieObj.getString(MovieMultipleJSONArray.TITLE);
                String overview = movieObj.getString(MovieMultipleJSONArray.OVERVIEW);
                String backdropUrl = movieObj.getString(MovieMultipleJSONArray.BACKDROP_PATH);
                String posterUrl = movieObj.getString(MovieMultipleJSONArray.POSTER_PATH);
                String release_date = movieObj.getString(MovieMultipleJSONArray.RELEASE_DATE);
                MovieDataModel movieDataObject = new MovieDataModel(id,overview,title,posterUrl, backdropUrl);

                movieDataObject.releaseDate = release_date;
                movieDataObject.genres = genres;

                double voteAvg      =  movieObj.getDouble(MovieMultipleJSONArray.VOTE_AVERAGE);
                movieDataObject.voteAvg = voteAvg;
                moviesList.add(movieDataObject);

            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        return moviesList;
    }

    //return array of genreIds from single movieJSONObj
    public static int[] getGenreArray(JSONObject movieJSONObj) throws JSONException {

        int[] genres = null;
        JSONArray genresJSONArray = movieJSONObj.getJSONArray(MovieMultipleJSONArray.GENRE_ID_Array);

        if (genresJSONArray.length() > 0) {
            genres = new int[genresJSONArray.length()];
            for (int g = 0; g < genresJSONArray.length(); g++) {
                genres[g] = genresJSONArray.getInt(g);
            }
        }
        return genres;
    }

    private class LoadMoreOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            for(int i=10; i<20; i++) display2.add(discover_moviesArrayList.get(i));

            display2 = addCreditsToList(display2);
            adapter.addItems(display2);
            movieListView.removeFooterView(loadMore);
            adapter.notifyDataSetChanged();
            MySingleton mySingleton = MySingleton.getInstance();
            mySingleton.setLoadMore(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        Log.d(TAG, "In onSaveInstanceState()");
    }

    @Override
    public void onPause() {
        Log.d(TAG, "In onPause(). Saving data...");
        MySingleton singleton = MySingleton.getInstance();
        singleton.saveData(discover_moviesArrayList);
        super.onPause();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "In onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "In onResume()");
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "In onStop()");
        super.onStop();
    }

    @Override
    public void onDestroyView() {


        super.onDestroyView();
        Log.d(TAG, "In onDestroyView()");
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.d(TAG, "In onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "In onDetach()");
    }

}
