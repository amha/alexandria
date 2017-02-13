/*
 * Copyright 2017 Amha Mogus. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    public static boolean IS_TABLET = false;
    private CharSequence title;
    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";

    AddBook addBookFragment;
    ListOfBooks listOfBooksFragment;
    About aboutFragment;

    private BroadcastReceiver messageReciever;
    ActionBarDrawerToggle toolbarToggle;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTablet()) {
            setContentView(R.layout.activity_main_tablet);
        } else {
            setContentView(R.layout.activity_main);
        }

        // Setup butter knife for efficient data binding
        ButterKnife.bind(this);

        // Setup navigation view, the toolbar, and their corresponding listeners
        setSupportActionBar(mToolbar);
        toolbarToggle = toggleHelper();
        mDrawerLayout.addDrawerListener(toolbarToggle);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        navigationUpdate(item);
                        return false;
                    }
                });

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        toolbarToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                if (toolbarToggle.onOptionsItemSelected(item)) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        toolbarToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() < 2) {
            finish();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("add books");
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d("AMHA", "Fragment is Null");
        }
    }

    // Helper method that loads a fragment that corresponds to
    // a navigation menu item click
    private void navigationUpdate(MenuItem menuItem) {

        // Preparing to swap out the current fragment
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Map the selected menu item to the relevant fragment
        switch (menuItem.getItemId()) {
            default:
            case R.id.MyBooks:
                listOfBooksFragment = new ListOfBooks();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, listOfBooksFragment, "book list")
                        .addToBackStack((String) title).commit();
                break;
            case R.id.AddBook:
                addBookFragment = new AddBook();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, addBookFragment, "add books")
                        .addToBackStack((String) title).commit();
                break;
            case 2:
                aboutFragment = new About();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, aboutFragment, "about")
                        .addToBackStack((String) title).commit();
                break;
        }
        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        mDrawerLayout.closeDrawers();
    }

    // Setup the toolbar toggle to animate the menu icon
    private ActionBarDrawerToggle toggleHelper() {
        return new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.about, R.string.books);
    }

    public void setTitle(int titleId) {
        title = getString(titleId);
    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(MESSAGE_KEY) != null) {
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

}