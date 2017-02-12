package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends ActionBarActivity {

    /**
     * Fragment managing the behaviors, interactions and
     * presentation of the navigation drawer.
     */
    // private NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use
     * in {@link #restoreActionBar()}.
     */
    private CharSequence title;

    String[] navigationMenuItems = {"My Books", "Add Book", "About"};

    public static boolean IS_TABLET = false;
    private BroadcastReceiver messageReciever;

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";

    AddBook addBookFragment;
    ListOfBooks listOfBooksFragment;
    About aboutFragment;

    @BindView(R.id.navigation_menu)
    ListView navigationList;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            restoreActionBar();
        } else {
            title = getTitle();
        }

        IS_TABLET = isTablet();
        if (IS_TABLET) {
            setContentView(R.layout.activity_main_tablet);
        } else {
            setContentView(R.layout.activity_main);
        }
        ButterKnife.bind(this);

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever, filter);

        navigationList.setAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, navigationMenuItems));

//        navigationDrawerFragment = (NavigationDrawerFragment)
//                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
//
//        // Set up the drawer.
//        navigationDrawerFragment.setUp(R.id.navigation_drawer,
//                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

//    @Override
//    public void onNavigationDrawerItemSelected(int position) {
//
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        //Fragment nextFragment;
//
//        switch (position) {
//            default:
//            case 0:
//                listOfBooksFragment = new ListOfBooks();
//                transaction
//                        .replace(R.id.container, listOfBooksFragment, "book list")
//                        .addToBackStack((String) title);
//                break;
//            case 1:
//                addBookFragment = new AddBook();
//                transaction
//                        .replace(R.id.container, addBookFragment, "add books")
//                        .addToBackStack((String) title);
//
//                break;
//            case 2:
//                aboutFragment = new About();
//                transaction
//                        .replace(R.id.container, aboutFragment, "about")
//                        .addToBackStack((String) title);
//                break;
//        }
//        transaction.commit();
//    }

    public void setTitle(int titleId) {
        title = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!navigationDrawerFragment.isDrawerOpen()) {
//            // Only show items in the action bar relevant to this screen
//            // if the drawer is not showing. Otherwise, let the drawer
//            // decide what to show in the action bar.
//            getMenuInflater().inflate(R.menu.main, menu);
//            restoreActionBar();
//            return true;
//        }
//        return super.onCreateOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }

//    @Override
//    public void onItemSelected(String ean) {
//        Bundle args = new Bundle();
//        args.putString(BookDetail.EAN_KEY, ean);
//
//        BookDetail fragment = new BookDetail();
//        fragment.setArguments(args);
//
//        int id = R.id.container;
//        if (findViewById(R.id.right_container) != null) {
//            id = R.id.right_container;
//        }
//        getSupportFragmentManager().beginTransaction()
//                .replace(id, fragment)
//                .addToBackStack("Book Detail")
//                .commit();
//
//    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(MESSAGE_KEY) != null) {
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void goBack(View view) {
        getSupportFragmentManager().popBackStack();
    }

    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //selectItem(position);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() < 2) {
            finish();
        }
        super.onBackPressed();
    }

//    /**
//     * Handle tapping the FAB.
//     */
//    @Override
//    public void addNewBook() {
//
//        // Check is Add Book fragment has been created or not
//        if (getSupportFragmentManager().findFragmentByTag("add books") != null) {
//            addBookFragment = (AddBook) getSupportFragmentManager()
//                    .findFragmentByTag("add books");
//        } else {
//            addBookFragment = new AddBook();
//        }
//
//        // Updated selected value of Navigation Drawer
//        //navigationDrawerFragment.selectItem(1);
//
//        // Load Add/Scan book fragment
////        getSupportFragmentManager().beginTransaction()
////                .replace(R.id.container, addBookFragment, "add books")
////                .addToBackStack((String) title)
////                .commit();
//    }

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
}