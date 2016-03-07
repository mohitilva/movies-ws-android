package com.hfad.moviesfun;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hfad.moviesfun.Utilities.backdropSizes;

import org.json.JSONException;
import org.json.JSONObject;
import com.hfad.moviesfun.Utilities.MovieDetailsJSONArray;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MovieDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MovieDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MovieDetailsFragment extends Fragment {

    private Context mContext;
    private static final String ARG_PARAM1 = "ID";
    private static final String ARG_PARAM2 = "BACKDROP_PATH";
    private static final String ARG_PARAM3 = "POSTER_PATH";
    private View fragmentView;

    private Utilities utils;

    private OkHttpClient client = new OkHttpClient();
    private String favStr;
    private String homepage;
    private String overview;
    private  String title;
    private int runtime;
    private float votes;
    private String posterPath;

    private String movieId;
    private String backdropPath;
    private OnFragmentInteractionListener mListener;

    private ImageView img;
    private TextView titleView;
    private TextView overviewView;
    private ImageView favoriteIcon;
    private TextView runTimeTextView;
    private TextView homePage;
    private static final String TAG="com.hfad.moviesfun.MovieDetailsFragment";

    Set<String> setFavorites;
    boolean isFav = false;
    SharedPreferences favorites;
    public static final String FAVORITE_PREFERENCES = "FAV_PREF";
    public static final String ITEM_CAT_MOVIES = "movies";

    public MovieDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).setCurrentFragment(MainActivity.fragmentTags.DETAILS);
    }

    public static MovieDetailsFragment newInstance(String id, String backDropPath, String posterPath) {
        MovieDetailsFragment fragment = new MovieDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, id);
        args.putString(ARG_PARAM2,backDropPath);
        args.putString(ARG_PARAM3,posterPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            movieId = getArguments().getString(ARG_PARAM1);
            backdropPath = getArguments().getString(ARG_PARAM2);
            posterPath = getArguments().getString(ARG_PARAM3);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = container.getContext();
        utils = new Utilities(mContext);

        //Set the views
        fragmentView = inflater.inflate(R.layout.movie_details_layout,null);
        img = (ImageView) fragmentView.findViewById(R.id.moviebackdrop_details);
        titleView = (TextView) fragmentView.findViewById(R.id.title_details);
        overviewView = (TextView) fragmentView.findViewById(R.id.overview_details);

        overviewView.setMovementMethod(new ScrollingMovementMethod());
        favoriteIcon = (ImageView) fragmentView.findViewById(R.id.imageView);
        homePage = (TextView) fragmentView.findViewById(R.id.homepage_textbox);
        homePage.setMovementMethod(LinkMovementMethod.getInstance());
        runTimeTextView = (TextView) fragmentView.findViewById(R.id.runtime);

        String requestUrl =  utils.getResourceUrl(String.valueOf(movieId));
        String backdropUrl = utils.getImageUrl(backdropPath, backdropSizes.w300.toString());
        Picasso.with(mContext).load(backdropUrl)
                .into(img, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError() {

                    }
                });
                //.into(img);

        String serviceResponse = null;

        try {
            serviceResponse = new ServiceResponseAsyncTask(client).execute(requestUrl).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //Parse the network response
        try {
            JSONObject responseJSONObj = new JSONObject(serviceResponse);
            homepage = responseJSONObj.getString(MovieDetailsJSONArray.HOMEPAGE);
            if (homepage == null | homepage.equals("")) homepage = "Not Available";
            runtime = responseJSONObj.getInt(MovieDetailsJSONArray.RUNTIME);
            votes = responseJSONObj.getLong(MovieDetailsJSONArray.VOTE_AVERAGE);
            overview =  responseJSONObj.getString(MovieDetailsJSONArray.OVERVIEW);
            title = responseJSONObj.getString(MovieDetailsJSONArray.TITLE);
        }  catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        //Set values for the views

        titleView.setText(title);
        overviewView.setText(overview);
        homePage.setText(homepage);
        runTimeTextView.setText(String.valueOf(runtime));

        setFavoriteIcon();

        favoriteIcon.setOnClickListener(new FavoriteIconOnClickListener());


        return fragmentView;

    }

    private class FavoriteIconOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(!isFav){
                isFav = true;
                favoriteIcon.setImageResource(android.R.drawable.star_on);
                setFavorites.add(String.valueOf(favStr));

            }
            else{

                isFav = false;
                favoriteIcon.setImageResource(android.R.drawable.star_off);
                setFavorites.remove(String.valueOf(favStr));

            }

            SharedPreferences.Editor editor = favorites.edit();
            editor.clear();
            editor.putStringSet(ITEM_CAT_MOVIES, setFavorites);
            Log.d(TAG, "set of Fav String:"+setFavorites);

            editor.apply();

          //  Toast.makeText(mContext, setFavorites.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setFavoriteIcon(){
        favorites = mContext.getSharedPreferences(FAVORITE_PREFERENCES, 0);
        setFavorites = favorites.getStringSet(ITEM_CAT_MOVIES, new HashSet<String>());
        favStr  = String.valueOf(movieId) + "|" + title + "|" + posterPath + "|" + backdropPath;

        if(setFavorites.contains(String.valueOf(favStr))){
            isFav = true;
            favoriteIcon.setImageResource(android.R.drawable.star_on);

        }else{
            isFav=false;
            favoriteIcon.setImageResource(android.R.drawable.star_off);
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
/*
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
*/
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}