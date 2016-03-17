package com.hfad.moviesfun.view;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hfad.moviesfun.R;
import com.hfad.moviesfun.adapters.DrawerAdapter;
import com.hfad.moviesfun.model.WSMetaData;
import com.hfad.moviesfun.utilities.Utilities;

import java.text.SimpleDateFormat;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity
        implements OnListItemClickCallback

{
    public static final SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy");


    private static String APP_TITLE = "MoviesFun";
    private static String FAV_TITLE = "My Favorites";
    private ListView drawerList;
    private String TAG = getClass().getName();
    private Context mContext;
    private Fragment fragment;
    private FragmentManager fragmentManager = getFragmentManager();
    private String currentFragment;
    private DrawerLayout mDrawerLayout;
    private android.support.v7.app.ActionBarDrawerToggle mDrawerToggle;
    private int drawerItemSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "In onCreate()");
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instantiateDrawer();

        if (savedInstanceState == null) {


            fragment = new MoviesRecentFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment, fragmentTags.MAIN.name())
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            currentFragment = fragmentTags.MAIN.name();

        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return false;
    }

    private void instantiateDrawer() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new MyDrawerListener());

        drawerList = (ListView) findViewById(R.id.drawer);

        DrawerAdapter drawerAdapter = new DrawerAdapter(mContext);

        drawerList.setAdapter(drawerAdapter);

        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        //create ActionBarDrawerToggle
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer, R.string.close_drawer) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();

            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

    }

    @Override
    public void updateActivityUI(String fragmentTag) {

        setActionBarTitle(fragmentTag);
        currentFragment = fragmentTag;

    }

    //when item is clicked on the navigation bar
    private void selectItem(int position) {

        drawerItemSelected = position;
        mDrawerLayout.closeDrawer(drawerList);
    }

    public void setActionBarTitle(String currentFragment) {

        String title;

        if(fragmentTags.MAIN.name().equals(currentFragment)){
            title = APP_TITLE;
            getSupportActionBar().setTitle(title);
        }else if(fragmentTags.FAVORITES.name().equals(currentFragment)){
            title = FAV_TITLE;
            getSupportActionBar().setTitle(title);
        }


    }

    public void setActionBarTitle(int position) {

        String title;

        switch (position) {

            case 1:
                title = APP_TITLE;
                getSupportActionBar().setTitle(title);
                break;
            case 2:
                title = FAV_TITLE;
                getSupportActionBar().setTitle(title);

        }

    }

    /* Implementing the callback for movie clicked. Called from two places, Main Fragment and Favorites Fragment */
    @Override
    public void onListItemClick(long id, String backDropPath, String posterPath, fragmentTags callingFragment) {

        fragment = new MovieDetailsFragment(String.valueOf(id), backDropPath, posterPath);
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment, fragmentTags.DETAILS.name())
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        fragmentManager.executePendingTransactions();
        currentFragment = fragmentTags.DETAILS.name();

    }

    @Override
    public void onBackPressed() {

        if (mDrawerLayout.isDrawerOpen(drawerList)) {
            mDrawerLayout.closeDrawer(drawerList);
            return;
        }

        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStackImmediate();
        } else {
            super.onBackPressed();
        }
    }

    private void switchFragment() {

        FragmentTransaction ft;
        ft = fragmentManager.beginTransaction();

        switch (drawerItemSelected) {
            case 0:
                return;
            case 1:

                if (currentFragment == fragmentTags.MAIN.name()) return;
                fragment = new MoviesRecentFragment();
                ft.replace(R.id.content_frame, fragment, fragmentTags.MAIN.name());
                ft.addToBackStack(null);
                break;

            case 2:
                if (currentFragment == fragmentTags.FAVORITES.name()) return;

                fragment = new FavoriteMovieFragment();
                ft.replace(R.id.content_frame, fragment, fragmentTags.FAVORITES.name());
                ft.addToBackStack(null);
                break;
        }

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
        fragmentManager.executePendingTransactions();
        setActionBarTitle(drawerItemSelected);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Log.d(TAG, "In onPostCreate()");
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "In onStop()");
        super.onStop();
    }

    @Override
    protected void onPostResume() {
        Log.d(TAG, "In onPostResume()");
        super.onPostResume();
    }


  /*
      When the user exits thru the back button clear the saved data so that the user gets fresh data when uses the app again.
      onDestroy is not called when user presses the home button on android,  onPause() and onStop() is called.
      When the user exits thru back button onDestroy() is also called.
      If the data is singleton is not cleared here, then the app uses the same data even after onDestroy() is called.
      In this case user would have to swipe the app out from the recent apps list to refresh the content.
  */

    @Override
    protected void onDestroy() {
        Log.d(TAG, "In onDestroy(). Clearing saved data");
        MoviesListSingleton singleton = MoviesListSingleton.getInstance();
        singleton.clearData();
        super.onDestroy();
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "In onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "In onResume()");
        super.onResume();
    }

    public  enum fragmentTags {
        MAIN,
        FAVORITES,
        DETAILS
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            selectItem(position);

        }
    }

    private class MyDrawerListener implements DrawerLayout.DrawerListener {

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(View drawerView) {

        }

        @Override
        public void onDrawerClosed(View drawerView) {
            switchFragment();
        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    }
}