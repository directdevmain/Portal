package com.directdev.portal.ui.finance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.directdev.portal.R;
import com.directdev.portal.tools.database.JournalDB;
import com.directdev.portal.tools.uihelper.MainViewPagerAdapter;
import com.directdev.portal.ui.WebappActivity;
import com.directdev.portal.ui.access.LoginAuthorization;

import java.text.NumberFormat;
import java.util.Locale;

public class FinanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String totalCharge;
        JournalDB db = new JournalDB(this);
        setContentView(R.layout.activity_finance);
        Toolbar toolbar = (Toolbar) findViewById(R.id.finance_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.finance_collapsing_toolbar);
        totalCharge = NumberFormat.getNumberInstance(Locale.US).format(db.sumFinance());
        collapsingToolbar.setTitle("Rp " + totalCharge + ".-");

        ViewPager viewPager = (ViewPager) findViewById(R.id.finance_viewpager);
        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.finance_tab);
        tabLayout.setupWithViewPager(viewPager);

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("View finance")
                .putContentType("Activity")
                .putContentId("studentData"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_finance, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_finance_webapp:
                SharedPreferences sPref = getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
                if (sPref.getBoolean(getString(R.string.is_no_session),true)){
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Refresh session to load new data", Snackbar.LENGTH_LONG)
                            .setAction("REFRESH", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(FinanceActivity.this, LoginAuthorization.class);
                                    startActivity(intent);
                                }
                            })
                            .setActionTextColor(Color.YELLOW);
                    snackbar.show();
                }else {
                    Intent intent = new Intent(this, WebappActivity.class);
                    intent.putExtra("url", "https://newbinusmaya.binus.ac.id/student/#/financial/financialStatus");
                    intent.putExtra("title", "Financial Status");
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new BillingFragment(), "BILLING");
        adapter.addFrag(new PaymentFragment(), "PAYMENT");
        viewPager.setAdapter(adapter);
    }
}
