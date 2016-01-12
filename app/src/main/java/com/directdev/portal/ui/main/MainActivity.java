package com.directdev.portal.ui.main;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.directdev.portal.R;
import com.directdev.portal.tools.event.RecyclerClickEvent;
import com.directdev.portal.tools.event.UpdateErrorEvent;
import com.directdev.portal.tools.event.UpdateFinishEvent;
import com.directdev.portal.tools.helper.MainViewPagerAdapter;
import com.directdev.portal.tools.helper.Portal;
import com.directdev.portal.tools.helper.Pref;
import com.directdev.portal.tools.model.Resource;
import com.directdev.portal.ui.access.LoginActivity;
import com.directdev.portal.ui.access.WebappActivity;
import com.directdev.portal.ui.main.account.AccountFragment;
import com.directdev.portal.ui.main.journal.JournalFragment;
import com.directdev.portal.ui.main.resource.ResourceFragment;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.util.Collections;

import de.greenrobot.event.EventBus;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * This is where Portal starts
 * When the application is opened, MainActivity will be launched
 */

public class MainActivity extends AppCompatActivity {

    private Realm realm;
    final private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123;
    private int textToShow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets up the toolbar
        setTitle("Portal");
        Toolbar toolbar = (Toolbar) findViewById(R.id.tabanim_toolbar);
        setSupportActionBar(toolbar);

        //Sets up the tabs
        ViewPager viewPager = (ViewPager) findViewById(R.id.tabanim_viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabanim_tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Analytics to track crashes, user growth and so on
        Portal application = (Portal) getApplication();
        EventBus.getDefault().register(this);
        realm = Realm.getDefaultInstance();
        textToShow = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLogin();
    }

    //This creates the menu (Notification, news, and open in webApp) using the menu_main layout
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // This is triggered when one of the menu button is pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_notification:
                //Show toast (the writing that pops up on the bottom) when notification clicked
                Toast notif = Toast.makeText(this, "Notification is still being built", Toast.LENGTH_SHORT);
                notif.show();
                return true;
            case R.id.toolbar_news:
                //Show toast (the writing that pops up on the bottom) when news clicked
                Toast news = Toast.makeText(this, "News is still being built", Toast.LENGTH_SHORT);
                news.show();
                return true;
            case R.id.action_schedule_webapp:
                //Launched the activity to open webapp when "open in webapp" clicked
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                if (isConnected) {
                    Intent intent = new Intent(this, WebappActivity.class);
                    intent.putExtra("url", "https://newbinusmaya.binus.ac.id/student/index.html#/learning/lecturing");
                    intent.putExtra("title", "Schedules");
                    startActivity(intent);
                } else {
                    Toast connection = Toast.makeText(this, "You are offline, please find a connection", Toast.LENGTH_SHORT);
                    connection.show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // This is used to setup the tabs
    private void setupViewPager(ViewPager viewPager) {
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(getSupportFragmentManager());
        new JournalFragment();
        new ResourceFragment();
        new AccountFragment();
        adapter.addFrag(new JournalFragment(), "JOURNAL");
        adapter.addFrag(new ResourceFragment(), "INFO");
        adapter.addFrag(new AccountFragment(), "ACCOUNT");
        viewPager.setAdapter(adapter);
    }

    // This is used to check if the user has logged in or not, if not than the login page will be shown
    public void checkLogin() {
        if (Pref.read(this, R.string.login_data_given_pref, 0) != 1 || Pref.read(this, R.string.login_condition_pref, 0) != 1) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        //Clean up all things that is opened or registered
        EventBus.getDefault().unregister(this);
        realm.close();
        super.onDestroy();
    }

    //This is called when a user click an item in journal to show the download card
    public void onEventMainThread(final RecyclerClickEvent event) {
        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("Main material")
                .putContentType("Bottom Sheet")
                .putContentId("1"));
        BottomSheetLayout bottomSheet = (BottomSheetLayout) findViewById(R.id.activity_main_bottomSheet);
        bottomSheet.showWithSheetView(LayoutInflater.from(this).inflate(R.layout.extended_info_sheet,bottomSheet,false));
        String details = event.schedule.getClasscode() + "  ||  Week " + event.schedule.getWeek() + "  ||  Session " + event.schedule.getSession();
        CardView download = (CardView) bottomSheet.findViewById(R.id.extended_info_download);

        TextView title = (TextView) bottomSheet.findViewById(R.id.extended_info_title);
        TextView detailTextView = (TextView) bottomSheet.findViewById(R.id.extended_info_details);
        title.setText(event.schedule.getCourseName());
        detailTextView.setText(details);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isNetworkAvailable()){
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("We need write access to download the file")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create()
                                    .show();

                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                        }
                    }else{
                        try {
                            RealmResults<Resource> resources = realm.where(Resource.class)
                                    .equalTo("description", event.schedule.getCourseID().substring(0, 8))
                                    .equalTo("mediaTypeId",1)
                                    .equalTo("courseOutlineTopicID",Integer.toString(event.schedule.getSession()))
                                    .findAll();

                            if(!resources.isEmpty()){
                                Resource resource = resources.get(0);
                                String link = resource.getPath() + resource.getLocation() + "/" + resource.getFilename();
                                link = link.replace("\\","/");
                                link = link.replace(" ","%20");
                                Log.d("Link", link);
                                Uri uri = Uri.parse(link);
                                DownloadManager.Request request = new DownloadManager.Request(uri);
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resource.getFilename());
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                dm.enqueue(request);
                            }else{
                                Toast updating = Toast.makeText(MainActivity.this, "No data found, try to refresh your data", Toast.LENGTH_SHORT);
                                updating.show();
                            }
                        }catch (IllegalStateException e){
                            Toast updating = Toast.makeText(MainActivity.this, "Still refreshing data, please wait", Toast.LENGTH_SHORT);
                            updating.show();
                        }
                    }
                }else {
                    Toast.makeText(MainActivity.this,"Don't be silly",Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this,"You have no internet connection",Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this,"You can only download when there is a connection",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void onEventMainThread(final UpdateErrorEvent event) {
        switch (textToShow){
            case 0:{
                Toast.makeText(this,"Failed to connect, just ain't your day",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                // TODO: Use your own attributes to track content views in your app
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("0"));

                break;
            }
            case 1:{
                Toast.makeText(this,"Still failed to connect, i feel you man",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"keep trying",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("1"));
                break;
            }case 2:{
                Toast.makeText(this,"Fails again and again",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Still fails to connect",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("2"));
                break;
            }case 3:{
                Toast.makeText(this,"Failure is the beginning of success",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"At least that's what people says",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"We still fails to connect to server",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("3"));
                break;
            }case 4:{
                Toast.makeText(this,"Failed to connect, maybe you should stop",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"There doesn't seems to be many hope left",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Failed to connect to server.... again",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("4"));
                break;
            }
            case 5:{
                Toast.makeText(this,"Looks like the server hates you...",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Or maybe it's your phone that hates you....",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Either way,",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"failed to connect to server",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Sorry...",Toast.LENGTH_SHORT).show();
                textToShow++;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("5"));
                break;
            }case 6:{
                Toast.makeText(this,"Let me tell you something",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Why did the chicken cross the road?",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Because.....",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"The chicken...",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Can't connect to the server",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"So he search for some signal",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"because there is no signal",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"Just like your condition",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"So, find a road...",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"And cross it",Toast.LENGTH_SHORT).show();
                Toast.makeText(this,"Then you probably will find some hope",Toast.LENGTH_LONG).show();
                Toast.makeText(this,"Still can't connect, Sorry...",Toast.LENGTH_LONG).show();
                textToShow = 0;
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("See Toast")
                        .putContentType("Toast")
                        .putContentId("6"));
                break;
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
