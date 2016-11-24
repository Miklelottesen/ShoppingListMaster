package com.junkiesoup.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.picasso.Picasso;

import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

public class MainActivity extends AppCompatActivity implements ConfirmDeleteDialogFragment.OnPositiveListener, NavigationView.OnNavigationItemSelectedListener {
    // For the delete dialog
    static ConfirmDeleteDialogFragment dialog;
    public Context context;

    public static Resources resources; // Declare resources object, to be used when accessing string resources in other classes
    public int productToEdit; // For deleting through context menu - will store the bag position of the item to delete

    // Declare adapter, listView, bag and df (the "delete checked" FAB)
    public FirebaseListAdapter adapter;
    public ListView listView;
    static ArrayList<Product> bag = new ArrayList<>();
    static ArrayList<Product> backupBag = new ArrayList<>();
    public ArrayList<User> users = new ArrayList<>();
    public DatabaseReference ref;
    FloatingActionButton df;

    // Firebase
    FirebaseListAdapter<Product> mAdapter;

    // User info
    public String userID;
    public String userName;
    public String userEmail;
    public String userPhoto;
    public Boolean loggedIn;

    // Navigation drawer
    public ImageView drawerProfilePic;
    public TextView drawerProfileName;
    public TextView drawerProfileEmail;

    // Boolean to momentarily disable the timer
    public boolean timerUpdateEnabled = true;

    int currentUser = 1; // TEST: sets ID for current user

    //AlertDialog.Builder builder = new AlertDialog.Builder(this);

    // Function for getting the adapter
    /*public FirebaseListAdapter getMyAdapter()
    {
        return adapter;
    }*/

    ArrayList<Product> postDeleteList;

    // Animation
    Animation animFadeOut;
    Animation animFadeOutDelete;

    FirebaseRemoteConfig firebaseRemoteConfig;

    /**
     * onCreate begin:
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null){ // Load saved Parcel to bag, if exists
            bag = savedInstanceState.getParcelableArrayList("arraylist");
        }
        setContentView(R.layout.activity_main);

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)  //set to false when releasing
                        .build();

        Map<String,Object> defaults = new HashMap<>();
        defaults.put("app_name",getResources().getString(R.string.app_name));
        firebaseRemoteConfig.setDefaults(defaults);

        Task<Void> myTask = firebaseRemoteConfig.fetch(1);

        myTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    firebaseRemoteConfig.activateFetched();
                    String name = firebaseRemoteConfig.getString("app_name");
                    getSupportActionBar().setTitle(name);
                } else
                    Log.d("ERROR","Task not succesfull + "+task.getException());
            }
        });

        postDeleteList = new ArrayList<>();

        //builder.setView(findViewById(R.id.edit_product_view));

        // Android Universal Image Loader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // User stuff
        loggedIn = false;
        userEmail = "test@gmail.com";
        userName = "User";
        userPhoto = "http://www.zerohedge.com/sites/default/files/images/user230519/imageroot/Trump_0.jpg";

        // Get resources for use in other classes
        resources = getResources();

        // Create test users
        users.add(new User("Mikkel",1));
        users.add(new User("Tabitha",2));



        // Load animations for list items slide
        //LayoutAnimationController layoutAnimation = AnimationUtils.loadLayoutAnimation(getApplicationContext(), R.anim.card_out)
        animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slide_out);
        animFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                itemSubmission(adapter);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animFadeOutDelete = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slide_out_delete);
        animFadeOutDelete.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                //itemSubmission(adapter);
                continueDeletion();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        // Toolbar stuff
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide the listview per default (will automatically be shown if it contains items)
        findViewById(R.id.list).setVisibility(View.GONE);

        // Run the method updateProducts every 30 secs (updates the display dates on the cards)
        new Timer().scheduleAtFixedRate(updateProducts, 10, 30000);

        // Remove initial focus from text input
        findViewById(R.id.mainLayout).requestFocus();

        // Get the text input
        EditText editText = (EditText) findViewById(R.id.itemInput);
        //editText.setInputType(InputType.TYPE_CLASS_TEXT);
        // Override default action when pressing enter on keyboard
        // Instead of adding a new line, I want the app to add a new item (but NOT remove focus from input field)
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addItem();
                    handled = true;
                }
                return handled;
            }
        });

        //getting the listiew
        listView = (ListView) findViewById(R.id.list);
        //adapter = new ProductInfoAdapter(this, bag); // Custom adapter

        //setting the adapter on the listview
        //listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Listener for clicking on a list item
        /*listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> l, View view, int position, long id) {
                        Log.d("Item click",(position+1)+" < "+adapter.getCount()+"?");
                        if(position+1 < adapter.getCount()) {
                            view.startAnimation(animFadeOut);
                        }
                        // Get listitem in question, set it to "checked" and submit to the adapter
                        //Product i = bag.get(position);
                        Product i = (Product) adapter.getItem(position);
                        i.setChecked();
                        if(position+1 >= adapter.getCount()) itemSubmission(adapter);
                        // itemsubmissionadapter
                    }
                }
        );*/

        /*listView.setOnTouchListener(
                new OnSwipeTouchListener(MainActivity.this){
                    public void onSwipeRight() {
                        Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
                    }
                    public void onSwipeLeft() {
                        Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                    }
                }
        );*/

        // Register the list view for longclick context menu
        //registerForContextMenu(listView);

        // Additional override of onItemLongClick, so the position will be stored in the productToEdit var
        // (otherwise the context menu wouldn't know which item to alter)
        listView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener()
                {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> l, View view, int position, long id) {
                        /*editMode = true;
                        productToEdit = position;
                        view.findViewById(R.id.edit_mode).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.view_mode).setVisibility(View.GONE);*/
                        return false;
                    }
                }
        );

        // Get the "delete" FAB
        df = (FloatingActionButton) findViewById(R.id.fabDelete);

        // Create listener for the "delete" FAB
        if(df != null)
            df.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view){
                    // Create a new temporary ArrayList, store (only) unchecked products in it, clear the old bag and add the temp list to it
                    ArrayList<Product> n = new ArrayList<>();
                    backupBag.clear();
                    for (Product p : bag) {
                        if(!p.isChecked()){
                            n.add(p);
                        } else {
                            backupBag.add(p);
                        }
                    }
                    postDeleteList.clear();
                    postDeleteList.addAll(n);
                    continueDeletion();
                    /*
                    bag.clear();
                    adapter.notifyDataSetChanged();
                    bag.addAll(n);
                    itemSubmission(adapter);
                    makeSnackbar(5000);
                    */

                }

            });
        // Drawer layout
        Toolbar appToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(appToolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, appToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Drawer profile elements
        /*drawerProfilePic = (ImageView) findViewById(R.id.drawer_profile_pic);
        drawerProfileName = (TextView) findViewById(R.id.drawer_profile_name);
        drawerProfileEmail = (TextView) findViewById(R.id.drawer_profile_email);*/
        View headerLayout = navigationView.inflateHeaderView(R.layout.nav_header_main);
        drawerProfilePic = (ImageView) headerLayout.findViewById(R.id.drawer_profile_pic);
        drawerProfileEmail = (TextView) headerLayout.findViewById(R.id.drawer_profile_email);
        drawerProfileName = (TextView) headerLayout.findViewById(R.id.drawer_profile_name);

        // Hide the "delete" FAB (will be shown automatically if there are checked items)
        //df.setVisibility(View.GONE);

        // Determine visibility of the message that appears in empty list (also called on every itemSubmission())
        emptyListMessage();

        // Get the "add" button
        ImageButton addButton = (ImageButton) findViewById(R.id.addButton);
        // Listener for clicking the "add" button
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
                findViewById(R.id.mainLayout).requestFocus(); // Remove focus from input field upon submission

                // Dismiss keyboard
                Utils.hideKeyboard(MainActivity.this);
            }
        });

        /**
         * Firebase
         */
        // Authentification (uses Google account)
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in

            //Log.d("Auth","Signed in as "+auth.getCurrentUser().getEmail());
            userID = auth.getCurrentUser().getUid();
            userName = auth.getCurrentUser().getDisplayName();
            userEmail = auth.getCurrentUser().getEmail();
            userPhoto = auth.getCurrentUser().getPhotoUrl().toString();
            String[] parts = userPhoto.split("/");
            userPhoto = "";
            for (int i = 0; i < parts.length-1; i++){
                userPhoto += parts[i]+"/";
            }
            loggedIn = true;
            updateProfileFields();
            Log.d("Logged in as","ID: "+userID+"\nName: "+userName+"\nEmail: "+userEmail+"\nPhoto url: "+userPhoto);
        } else {
            // not signed in
            Log.d("Auth","Not signed in");
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                            .build(),
                    RC_SIGN_IN);
        }
        adapter = adapterInit();

        listView.setAdapter(adapter);
        itemSubmission(adapter);

    } /** onCreate end */

    public void updateProfileFields(){
        if(loggedIn){
            View v = findViewById(R.id.drawer_profile_pic);
            ImageLoader imageLoader = ImageLoader.getInstance();
            //ImageView dPP = (ImageView) findViewById(R.id.drawer_profile_pic);
            Log.d("WTFF?",drawerProfilePic.toString());
            imageLoader.displayImage(userPhoto+"", drawerProfilePic);

            drawerProfileName.setText(userName);
            drawerProfileEmail.setText(userEmail);
        }
    }

    private FirebaseListAdapter adapterInit (){
        Log.d("Squash that bug!","1. Getting FB reference");
        ref = FirebaseDatabase.getInstance().getReference().child("item");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                itemSubmission(adapter);
                Log.d("Firebase Child","Added");
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                itemSubmission(adapter);
                Log.d("Firebase Child","Changed");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                itemSubmission(adapter);
                makeSnackbar(5000);
                Log.d("Firebase Child","Removed");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                itemSubmission(adapter);
                Log.d("Firebase Child","Moved");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                itemSubmission(adapter);
                Log.d("Firebase Child","Cancelled");
            }
        });
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    itemSubmission(adapter);
                    System.out.println("connected");
                } else {
                    System.out.println("not connected");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Listener was cancelled");
            }
        });
        Log.d("Squash that bug!","2. Initializing adapter");
        adapter = new FirebaseListAdapter<Product>(this, Product.class, R.layout.product_card, ref.orderByChild("checked_date")) {
            @Override
            protected void populateView(View convertView, Product p, int position) {
                Log.d("Squash that bug!","3. Getting item for position "+position);
                // Get the data item for this position
                final Product product = getItem(position);
                Log.d("Squash that bug!","4. Determine whether to reuse or inflate view");
                // Check if an existing view is being reused, otherwise inflate the view
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.product_card, null, false);
                }
                //TextView productInfoView1 = (TextView) convertView.findViewById(R.id.playerInfoText1);
                Log.d("Squash that bug!","5. Find 'user' field");
                TextView productAuthor = (TextView) convertView.findViewById(R.id.user);

                Log.d("Productadd","Adding "+product.getName());
                Log.d("Productadd","At "+product.fetchDate());

                // Get the username

                // Set string to username
                String uName = "User not found";
                for (User u : users) {
                    if(u.getId() == product.getAuthor()){
                        uName = u.getName();
                    }
                }
                productAuthor.setText(uName);

                // Set context menu listener
                //convertView.setOnCreateContextMenuListener(this);

                TextView productName = (TextView) convertView.findViewById(R.id.name);
                productName.setText(product.getName());

                TextView productDate = (TextView) convertView.findViewById(R.id.date);
                productDate.setText(setDateDisplay(product.fetchDate()));

                CheckBox productChecked = (CheckBox) convertView.findViewById(R.id.marked);
                if(product.isChecked()){
                    productChecked.setChecked(true);
                } else {
                    productChecked.setChecked(false);
                }

                ImageView productSeen = (ImageView) convertView.findViewById(R.id.seen);
                if(product.getSeen()) {
                    productSeen.setVisibility(View.INVISIBLE);
                }
                else {
                    productSeen.setVisibility(View.VISIBLE);

                }

                listView.setOnItemClickListener(
                        new AdapterView.OnItemClickListener()
                        {
                            @Override
                            public void onItemClick(AdapterView<?> l, View view, int position, long id) {
                                Log.d("Item click",(position+1)+" < "+adapter.getCount()+"?");
                                if(position+1 < adapter.getCount()) {
                                    view.startAnimation(animFadeOut);
                                }
                                // Get listitem in question, set it to "checked" and submit to the adapter
                                //Product i = bag.get(position);
                                Product i = (Product) adapter.getItem(position);
                                i.setChecked();
                                adapter.getRef(position).setValue(i);
                                if(position+1 >= adapter.getCount()) itemSubmission(adapter);
                                // itemsubmissionadapter
                            }
                        }
                );
            }

        };
        return adapter;
    }

    // Method for creating a user friendly date string, based on a Date object
    private String setDateDisplay(Long date){
        Date d = new Date(date*-1);
        String returnString = MainActivity.resources.getString(R.string.date_not_found);

        // Get current date
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Date clt = cal.getTime(); // Current local time

        /**
         * Compare Date d with current date
         */
        // Set date formats for comparison
        DateFormat[] dateFormats = new DateFormat[5]; // Array to contain date formats, making looping available
        dateFormats[0] = new SimpleDateFormat("yyyy");
        dateFormats[1] = new SimpleDateFormat("MM");
        dateFormats[2] = new SimpleDateFormat("dd");
        dateFormats[3] = new SimpleDateFormat("HH");
        dateFormats[4] = new SimpleDateFormat("mm");
        // Loop through date formats and set timezone
        for(int i = 0; i < dateFormats.length; i++){
            dateFormats[i].setTimeZone(TimeZone.getDefault());
        }



        // Find the date/time difference
        String logID = "Date";
        int yearNow = di(dateFormats[0],clt);
        Log.d("Date bug",dateFormats[0].toString()+", "+d.toString());
        int yearD = di(dateFormats[0],d);
        int monthNow = di(dateFormats[1],clt);
        int monthD = di(dateFormats[1],d);
        //Log.d(logID,yearNow+" - "+yearD+" = "+(yearNow-yearD));
        if(yearNow > yearD && monthNow>=monthD){
            // The product was added/modified a year or more ago
            int diff = yearNow - yearD;
            // Handle singular, plural return strings (should it be "year" or "years"?)
            if(diff == 1){
                returnString = "1 "+MainActivity.resources.getString(R.string.date_year);
            } else {
                returnString = diff+" "+MainActivity.resources.getString(R.string.date_years);
            }
        } else {
            //Log.d(logID,monthNow+" - "+monthD+" = "+(monthNow-monthD));
            int monthAdd = 0;

            if(monthNow > monthD){
                // The product was added/modified a month or more ago
                int diff = monthNow - monthD;
                if(yearNow<yearD) diff = monthNow - (monthD-12);
                // If the difference is less than 3 months, it'll display the date (dd/MM)
                // If not, it'll display "X months ago."
                if(diff < 3){
                    returnString = (new SimpleDateFormat("dd/MM")).format(d);
                } else {
                    returnString = diff+" "+MainActivity.resources.getString(R.string.date_months);
                }
            } else {
                int dayNow = di(dateFormats[2],clt);
                int dayD = di(dateFormats[2],d);
                //Log.d(logID,dayNow+" - "+dayD+" = "+(dayNow-dayD));
                if(dayNow > dayD){
                    // The product was added/modified a day ago or more
                    int diff = dayNow - dayD;
                    // If the difference is less than 4 days, it'll display "X days ago"
                    // If not, it'll display the date (dd/MM)
                    if(diff < 4){
                        // Singular, plural corrections
                        if(diff == 1){
                            returnString = diff+" "+MainActivity.resources.getString(R.string.date_day);
                        } else {
                            returnString = diff+" "+MainActivity.resources.getString(R.string.date_days);
                        }
                    } else {
                        returnString = (new SimpleDateFormat("dd/MM")).format(d);
                    }
                } else {
                    int hourNow = di(dateFormats[3],clt);
                    int hourD = di(dateFormats[3],d);
                    //Log.d(logID,hourNow+" - "+hourD+" = "+(hourNow-hourD));
                    if(hourNow > hourD){
                        // The product was added/modified an hour ago or more
                        int diff = hourNow - hourD;
                        // If the difference is less than 4 hours, it'll display "X hours ago"
                        // If not, it'll display the time (HH:mm)
                        if(diff < 4){
                            // Singular, plural corrections
                            if(diff == 1){
                                returnString = diff+" "+MainActivity.resources.getString(R.string.date_hour);
                            } else {
                                returnString = diff+" "+MainActivity.resources.getString(R.string.date_hours);
                            }
                        } else {
                            returnString = (new SimpleDateFormat("HH:mm")).format(d);
                        }
                    } else {
                        int minuteNow = di(dateFormats[4],clt);
                        int minuteD = di(dateFormats[4],d);
                        //Log.d(logID,minuteNow+" - "+minuteD+" = "+(minuteNow-minuteD));
                        if(minuteNow > minuteD){
                            // The product was added/modified a minute ago or more
                            int diff = minuteNow - minuteD;
                            // Display either "X minute ago"/"X minutes ago" or "Just now", if diff=0
                            if(diff > 1){
                                returnString = diff+" "+MainActivity.resources.getString(R.string.date_minutes);
                            } else if(diff == 1){
                                returnString = "1 "+MainActivity.resources.getString(R.string.date_minute);
                            }
                        } else {
                            returnString = MainActivity.resources.getString(R.string.date_just_now);
                        }
                    }
                }
            }
        }

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        dateFormat.setTimeZone(TimeZone.getDefault());
        return returnString;
    }

    // Method for returning Date values as int value
    public int di (DateFormat f, Date D){
        String s = f.format(D);
        int r = Integer.parseInt(s);
        return r;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_myLists) {
            // Handle the camera action
        } else if (id == R.id.nav_addList) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Methods:
     */

    public void continueDeletion(){
        bag.clear();
        adapter.notifyDataSetChanged();
        bag.addAll(postDeleteList);
        ref.removeValue();
        itemSubmission(adapter);
        makeSnackbar(5000);
    }
    public void deleteChecked(){
        // Prepare bag to store deleted items for recovery
        bag.clear();
        for (int i = 0; i < adapter.getCount(); i++){
            Product p = (Product)adapter.getItem(i);
            if(p.isChecked()){
                // Add to bag
                bag.add(p);
                // Remove from adapter (onChildRemoved callback mathod will handle submission and snackbar)
                adapter.getRef(i).setValue(null);
            }
        }
    }
    public void deleteAll(){
        // Prepare bag to store deleted items for recovery
        bag.clear();
        for (int i = 0; i < adapter.getCount(); i++){
            Product p = (Product)adapter.getItem(i);
            // Add to bag
            bag.add(p);
            // Remove from adapter (onChildRemoved callback mathod will handle submission and snackbar)
            adapter.getRef(i).setValue(null);
        }
    }
    public void restoreDeleted(){
        for(Product p : bag){
            ref.push().setValue(p);
        }
        bag.clear();
    }

    // TEST: Add a bunch of test items to the bag (call this function in onCreate, AFTER initializing bag and adapter)
    public void addTestItems(){
        /*bag.add(new Product("Køkkenrulle",new Date(2016-1900,9-1,17,14,10)));
        bag.add(new Product("Fryseposer",new Date(2016-1900,9-1,17,14,11)));
        bag.add(new Product("Afkalker",new Date(2015-1900,11-1,11,14,10)));
        bag.add(new Product("Hørfrø",new Date(2016-1900,11-1,2,14,10)));
        bag.add(new Product("Dadler",new Date(2016-1900,8-1,11,14,10)));
        bag.add(new Product("4 appelsiner",new Date(2016-1900,9-1,11,14,10)));
        bag.add(new Product("Sukker",new Date(2016-1900,9-1,21,9,10)));
        bag.add(new Product("Mel",new Date(2016-1900,9-1,21,15,10)));
        bag.add(new Product("Pistols"));*/
        ref.push().setValue(new Product("Margerine",new Date(2016-1900,9-1,17,14,10)));
        itemSubmission(adapter);
    }

    // Adding a new product to the bag, based on value of input field
    public void addItem(){
        // !!!!DELETE THIS:
        //int i = 10/0; // WILL make the app crash - just to test crash reporting
        // Get input field
        EditText itemToAdd = (EditText) findViewById(R.id.itemInput);
        if(itemToAdd.getText().toString() != "" && itemToAdd.getText().toString() != null){ // Prevent addition of empty products
            // Create new product based on value of input field and add it to bag
            Product newProduct = new Product(itemToAdd.getText().toString());
            //bag.add(newProduct);
            ref.push().setValue(newProduct);
            // Empty input field upon submission
            itemToAdd.setText("");
            itemSubmission(adapter);
        }
    }

    // Procedure for submitting items (sorting, then submitting) and showing/hiding the "delete checked" FAB
    public void itemSubmission(FirebaseListAdapter<Product> p){
        //p.sort(new DateDesc());
        //p.sort(new uncheckedFirst());
        ref.push().getDatabase();
        p.notifyDataSetChanged();
        //p.notifyAll();
        /*for(int i = 0; i < p.getCount(); i++){
            int n = p.getCount() - 1;
            int b = n - i;
            int t = i * -1;
            if(i<n){
                // Because the last item doesn't have an item after it to compare to
                Product p1 = p.getItem(i);
                Product p2 = p.getItem(i+1);
                if(p1.fetchDate().compareTo(p2.fetchDate()) == 1){
                    // p1 is older than p2
                    p.
                }
            }
        }*/

        // Determine whether or not to show the FAB for deleting products
        boolean display = false;
        /*for (Product prod : adapter) {
            display = (prod.isChecked()) || display;
        }*/
        for (int i = 0; i < p.getCount(); i++){
            Product prod = p.getItem(i);
            display = (prod.isChecked()) ? true : display;
            Log.d("Display FAB",prod.isChecked()+"");
        }
        Log.d("testDisplay",display+"");
        if(display){
            df.setVisibility(View.VISIBLE);
        } else {
            df.setVisibility(View.GONE);
        }
        emptyListMessage();
    }

    // Determine if the message "Your items will appear here..." should be shown, or the list
    public void emptyListMessage(){
        final View textView = (View) findViewById(R.id.emptyList);
        final View listView = (View) findViewById(R.id.list);
        final long transitionTime = 100;
        adapter = (adapter == null) ? adapterInit() : adapter;

        // Animated transition between empty list message and listview
        if(adapter.isEmpty() && !textView.isShown()) {
            listView.animate().alpha(0F).setDuration(transitionTime).setListener(null);
            Handler h = new Handler();
            Runnable hideIt = new Runnable (){
                public void run(){
                    listView.setVisibility(View.GONE);
                    textView.setVisibility(View.VISIBLE);
                    textView.animate().alpha(1F).setDuration(transitionTime).setListener(null);
                }
            };
            h.postDelayed(hideIt,transitionTime);
        }
        else if(!adapter.isEmpty() && textView.isShown()){
            textView.animate().alpha(0F).setDuration(transitionTime).setListener(null);
            Handler h = new Handler();
            Runnable hideIt = new Runnable (){
                public void run(){
                    textView.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    listView.animate().alpha(1F).setDuration(transitionTime).setListener(null);
                }
            };
            h.postDelayed(hideIt,transitionTime);
        }
    }

    /**
     * Comparator methods:
     */

    // Comparator that sorts the list by checked, unchecked first
    public class uncheckedFirst implements Comparator<Product> {
        @Override
        public int compare(Product p1, Product p2) {
            if(p1.isChecked()) {
                return 1;
            } else return -1;
        }

    }

    // Comparator that sorts the list by date, descending
    public class DateDesc implements Comparator<Product> {
        @Override
        public int compare(Product p1, Product p2) {
            return p1.fetchDate().compareTo(p2.fetchDate());
        }

    }

    /**
     * Dialogs:
     */
    public void showDialog(View v) {
        //showing our dialog.

        dialog = new ConfirmDialog();
        //Here we show the dialog
        //The tag "MyFragement" is not important for us.
        dialog.show(getFragmentManager(), "MyFragment");
    }

    public static class ConfirmDialog extends ConfirmDeleteDialogFragment {
        @Override
        protected void negativeClick() {
            // Nothing to see here
        }
    }

    @Override
    public void onPositiveClicked() {
        Log.d("Dialog","Positive clicked");
        /*backupBag.clear();
        backupBag.addAll(bag);
        adapter.cleanup();
        bag.clear();
        itemSubmission(adapter);
        makeSnackbar(6000);
        for (int i = 0; i < adapter.getCount(); i++){
            adapter.getRef(i).setValue(null);
            itemSubmission(adapter);
        }*/
        deleteAll();
    }

    /**
     * Snackbar:
     */
    // Snackbar that appears every time something
    // has been deleted, with option to undo
    public void makeSnackbar(int duration){
        // All delete actions (pick item/items to delete, clear backupBag,
        // add to backupBag, delete item/items) must be performed BEFORE
        // calling this function!

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        int delCount = bag.size(); // Number of deleted items
        String m = delCount+" "+((delCount == 1) // Make string with delCount and string values
                ? getString(R.string.snackbar_text_single)
                : getString(R.string.snackbar_text_plural));

        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.mainLayout), m, duration)
                .setAction(getString(R.string.snackbar_undo), new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        /*bag.addAll(backupBag);
                        backupBag.clear();
                        itemSubmission(adapter);
                        Log.d("Snackbar","Snackbar came!");*/
                        restoreDeleted();
                        findViewById(R.id.fabDelete).setVisibility(View.GONE); // Hide the 'delete' FAB while snackbar is visible

                        Snackbar snackbar = Snackbar.make(findViewById(R.id.mainLayout), getString(R.string.snackbar_restored), Snackbar.LENGTH_SHORT)
                                .setCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);
                                        Log.d("Snackbar","Snackbar gone...");
                                        itemSubmission(adapter);
                                    }
                                });
                        snackbar.show();
                    }
                });
        snackbar.show();
    }

    /**
     * Menus:
     */

    // onCreate overrides
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Options menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, // Context menu for longclick on list items
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.product_menu, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Options->Settings
        if (id == R.id.action_settings) {
            return true;
        }

        // Options->Add test products
        if (id == R.id.action_add_test_products) {
            addTestItems();
            return true;
        }

        // Options->Mark all as unseen
        if (id == R.id.action_all_unseen) {
            /*ArrayList<Product> tempBag = new ArrayList<Product>();
            for (Product p : bag) {
                p.setSeen(false);
                tempBag.add(p);
            }
            bag.clear();
            bag.addAll(tempBag);
            itemSubmission(adapter);*/
            for (int i = 0; i < adapter.getCount(); i++){
                Product p = (Product) adapter.getItem(i);
                p.setSeen(false);
                adapter.getRef(i).setValue(p);
                itemSubmission(adapter);
            }
            return true;
        }

        // Options->Mark all as seen
        if (id == R.id.action_all_seen) {
            /*ArrayList<Product> tempBag = new ArrayList<Product>();
            for (Product p : bag) {
                p.setSeen(true);
                tempBag.add(p);
            }
            bag.clear();
            bag.addAll(tempBag);
            itemSubmission(adapter);*/
            for (int i = 0; i < adapter.getCount(); i++){
                Product p = (Product) adapter.getItem(i);
                p.setSeen(true);
                adapter.getRef(i).setValue(p);
                itemSubmission(adapter);
            }
            return true;
        }

        // Options->Clear all
        if (id == R.id.action_clear_all) {
            showDialog(findViewById(R.id.mainLayout));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {

            // Context menu->Edit
            case R.id.product_action_edit:
                //AlertDialog dialog = builder.create();
                return true;

            // Context menu->Delete
            case R.id.product_action_delete:
                backupBag.clear();
                backupBag.add(bag.get(productToEdit));
                bag.remove(productToEdit);
                itemSubmission(adapter);
                makeSnackbar(3000);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.cleanup();
    }

    /**
     * Timers:
     */
    public TimerTask updateProducts = new TimerTask() {
        @Override
        public void run() {
            // Timer that will update the displayed date on the product cards every 15 secs
            runOnUiThread(new Runnable() { // Required to alter views
                @Override
                public void run() {
                    if(!timerUpdateEnabled) {
                        Log.d("Timer","Refresh prevented by app.");
                        return;
                    }
                    // Temporary arraylist
                    /*ArrayList<Product> tempBag = new ArrayList<Product>();
                    // Loop through each Product in bag, update the display date and add to tempBag
                    for (Product p : bag) {
                        p.updateDisplayDate();
                        tempBag.add(p);
                        // Set the product to "seen" if it's posted by another user
                        if(!p.getSeen() && p.getAuthor() != currentUser){
                            p.setSeen(true);
                        }
                    }
                    bag.clear();
                    adapter.notifyDataSetChanged();
                    bag.addAll(tempBag);
                    itemSubmission(adapter);*/

                    for (int i = 0; i < adapter.getCount(); i++){
                        Product p = (Product) adapter.getItem(i);
                        if(!p.getSeen() && p.getAuthor() != currentUser) {
                            p.setSeen(true);
                        }
                    }
                    itemSubmission(adapter);
                }
            });
        }

    };


    /**
     * Save instance state:
     */
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        for (int i = 0; i < adapter.getCount(); i++){
            bag.clear();
            bag.add((Product)adapter.getItem(i));
        }
        outState.putParcelableArrayList("arraylist", bag);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState){
        super.onRestoreInstanceState(savedState);
        ListView listView = (ListView) findViewById(R.id.list);
        bag = savedState.getParcelableArrayList("arraylist");
        adapter = adapterInit();

        //setting the adapter on the listview
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        itemSubmission(adapter);
    }

}
