package com.junkiesoup.shoppinglist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;

import com.firebase.client.FirebaseError;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArraySet;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

//import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.view.View.GONE;
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
    public DatabaseReference listsRef;
    FloatingActionButton df;

    public Timer timer;

    // Firebase
    FirebaseListAdapter<Product> mAdapter;
    public FirebaseAuth auth;

    public Boolean init = true;

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
    String currentList;

    ArrayList<Product> postDeleteList;

    // Animation
    Animation animFadeOut;
    Animation animFadeOutDelete;

    FirebaseRemoteConfig firebaseRemoteConfig;

    SharedPreferences prefs;

    // Utilities
    public Utils utils;

    public ImageLoaderConfiguration config;

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

        prefs = getApplicationContext().getSharedPreferences("appSettings",0);
        currentList = prefs.getString("currentList",null);

        toggleQtyField();

        Log.d("Get prefs",currentList+"");

        utils = new Utils();

        postDeleteList = new ArrayList<>();

        //builder.setView(findViewById(R.id.edit_product_view));
        findViewById(R.id.emptyList).setVisibility(View.GONE);

        // Android Universal Image Loader
        config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // User stuff
        loggedIn = false;
        userEmail = "test@gmail.com";
        userName = "User";
        userPhoto = "";

        // Get resources for use in other classes
        resources = getResources();

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
        //findViewById(R.id.list).setVisibility(View.GONE);

        // Remove initial focus from text input
        findViewById(R.id.mainLayout).requestFocus();

        // Get the text input
        EditText editText = (EditText) findViewById(R.id.itemInput);
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
                    //ArrayList<Product> n = new ArrayList<>();
                    //backupBag.clear();
                    /*for (Product p : bag) {
                        if(!p.isChecked()){
                            n.add(p);
                        } else {
                            backupBag.add(p);
                        }
                    }*/
                    //postDeleteList.clear();
                    //postDeleteList.addAll(n);
                    deleteChecked();

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

        android.support.design.widget.NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Drawer profile elements
        View headerLayout = navigationView.inflateHeaderView(R.layout.nav_header_main);
        drawerProfilePic = (ImageView) headerLayout.findViewById(R.id.drawer_profile_pic);
        drawerProfileEmail = (TextView) headerLayout.findViewById(R.id.drawer_profile_email);
        drawerProfileName = (TextView) headerLayout.findViewById(R.id.drawer_profile_name);

        // Hide the "delete" FAB (will be shown automatically if there are checked items)
        //df.setVisibility(View.GONE);

        // Determine visibility of the message that appears in empty list (also called on every itemSubmission())
        //emptyListMessage();

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
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            //initAdapters();
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
        auth.addAuthStateListener(
                new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                        if(firebaseAuth.getCurrentUser() != null && init){
                            init = false;
                            // already signed in
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
                            Log.d("FBD","Updating profile fields");
                            updateProfileFields();
                            Log.d("FBD","ID: "+userID+"\nName: "+userName+"\nEmail: "+userEmail+"\nPhoto url: "+userPhoto);
                            initAdapters();
                        }
                    }
                }
        );


    } /** onCreate end */

    public void initAdapters(){
        Log.d("FBD","Initializing adapters");
        if(currentList == null) {
            listPicking();
        } else {
            adapter = adapterInit();
            listView.setAdapter(adapter);
        }
    }

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

    private void listPicking(final Boolean force){
        Log.d("FBD","Current list not found, creating ref");
        DatabaseReference listmanRef;
        listmanRef = FirebaseDatabase.getInstance().getReference().child("user/"+auth.getCurrentUser().getUid()+"/list");
        Log.d("FBD","Ref created, adding listener");
        final ArrayList<ListInstance> listmanPackage = new ArrayList<>();
        listmanRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Intent intent = new Intent(getApplicationContext(), ListmanActivity.class);

                Bundle listsinfo = new Bundle();
                ArrayList<ListInstance> li = new ArrayList<ListInstance>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d("FBD","Running a loop");
                    String dbKey = child.getKey();
                    String lName = child.child("name").getValue(String.class);
                    Long lDate = child.child("date").getValue(Long.class);
                    String valRef = child.getRef().toString();
                    valRef = valRef.split("/")[valRef.split("/").length-1];
                    Iterable fblUsers = child.child("users").getChildren();
                    ArrayList<String> lUsers = new ArrayList<String>();
                    for (Object u : fblUsers){
                        lUsers.add(u.toString());
                    }

                    li.add(new ListInstance(lName,lUsers,new ArrayList<Product>(),lDate, valRef));
                }
                listsinfo.putParcelableArrayList("listinstances", li);
                intent.putExtras(listsinfo);
                if(currentList == null || force) {
                    timer = null;
                    startActivityForResult(intent, 420);
                }
            }
            @Override
            public void onCancelled(DatabaseError dbError){
                Log.d("Db error",dbError.toString());
            }
        });
    }
    public void toggleQtyField(){
        Spinner qtyField = (Spinner) findViewById(R.id.annoyingmandatoryspinner);
        boolean showQtyField = prefs.getBoolean("show_qty_field",false);
        if(showQtyField) qtyField.setVisibility(View.VISIBLE);
        else qtyField.setVisibility(GONE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 420) { // From ListmanActivity
            if(resultCode == 200){
                String result = data.getStringExtra("result");
                currentList = result;
                Log.d("Pleasetellmewrong",result);
                initAdapters();
            } else if(resultCode == 300){
                String result = data.getStringExtra("result");
                DatabaseReference dbr = FirebaseDatabase.getInstance().getReference().child("user/"+auth.getCurrentUser().getUid()+"/list");
                DatabaseReference p = dbr.push();
                p.setValue(new ListInstance(result));
                Log.d("Pleasetellmewrong",p.getRef().toString().split("/")[p.getRef().toString().split("/").length-1]);
                String id = p.getRef().toString().split("/")[p.getRef().toString().split("/").length-1];
                currentList = id;
                initAdapters();
            }
            else {
                //Write your code if there's no result
            }
        } else if(requestCode == 1337) { // From SettingsActivity
            boolean showQty = MyPreferenceFragment.showQty(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("show_qty_field");
            editor.putBoolean("show_qty_field",showQty);
            editor.commit();
            toggleQtyField();
            initAdapters();
        }
        super.onActivityResult(resultCode, resultCode, data);
    }
    private void listPicking(){
        listPicking(false);
    }

    private FirebaseListAdapter adapterInit (){
        Log.d("Squash that bug!","1. Getting FB reference");
        auth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference().child("user/"+auth.getCurrentUser().getUid()+"/list/"+currentList+"/items");
        DatabaseReference refList = ref.getParent().child("name");
        refList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String listName = (String) dataSnapshot.getValue();
                getSupportActionBar().setTitle(listName);
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //adapter.notifyDataSetChanged();
                Log.d("Firebase Child","Added");
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //adapter.notifyDataSetChanged();
                Log.d("Firebase Child","Changed");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //adapter.notifyDataSetChanged();
                Log.d("Firebase Child","Removed");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                //adapter.notifyDataSetChanged();
                Log.d("Firebase Child","Moved");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //adapter.notifyDataSetChanged();
                Log.d("Firebase Child","Cancelled");
            }
        });
        /*DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
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
        });*/
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
                String uName;
                uName = product.getAuthor();
                productAuthor.setText(uName);

                // Set context menu listener
                //convertView.setOnCreateContextMenuListener(this);

                TextView productName = (TextView) convertView.findViewById(R.id.name);
                productName.setText(product.getName());

                TextView productDate = (TextView) convertView.findViewById(R.id.date);
                productDate.setText(utils.setDateDisplay(product.fetchDate()));

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
                if(adapter.getCount() < 1){
                    View textView = (View) findViewById(R.id.emptyList);
                    textView.setVisibility(View.GONE);
                } else {
                    View textView = (View) findViewById(R.id.emptyList);
                    textView.setVisibility(View.VISIBLE);
                }
            }

        };
        return adapter;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_myLists) {
            listPicking(true);
            return false;
        } else if (id == R.id.nav_addList) {
            Toast toast = Toast.makeText(getApplicationContext(),"Feature not implemented yet",Toast.LENGTH_LONG);
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_manage) {
            Toast toast = Toast.makeText(context,"Feature not implemented yet",Toast.LENGTH_LONG);
        } else if (id == R.id.nav_share) {
            Toast toast = Toast.makeText(context,"Feature not implemented yet",Toast.LENGTH_LONG);
        } else if (id == R.id.nav_send) {
            Toast toast = Toast.makeText(context,"Feature not implemented yet",Toast.LENGTH_LONG);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Methods:
     */

    private void continueDeletion(){
        bag.clear();
        adapter.notifyDataSetChanged();
        bag.addAll(postDeleteList);
        ref.removeValue();
        itemSubmission(adapter);
        makeSnackbar(5000);
    }
    private void deleteChecked(){
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
        makeSnackbar(5000);
    }
    private void deleteAll(){
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
    private void restoreDeleted(){
        for(Product p : bag){
            ref.push().setValue(p);
        }
        bag.clear();
    }

    // TEST: Add a bunch of test items to the bag (call this function in onCreate, AFTER initializing bag and adapter)
    private void addTestItems(){
        ref.push().setValue(new Product("Margerine",new Date(2016-1900,9-1,17,14,10)));
        itemSubmission(adapter);
    }

    // Adding a new product to the bag, based on value of input field
    private void addItem(){
        // !!!!DELETE THIS:
        //int i = 10/0; // WILL make the app crash - just to test crash reporting
        // Get input field
        EditText itemToAdd = (EditText) findViewById(R.id.itemInput);
        if(itemToAdd.getText().toString() != "" && itemToAdd.getText().toString() != null){ // Prevent addition of empty products
            // Get annoyingmandatoryspinner
            // TODO: DELETE SPINNER IMPLEMENTATION FOR PRODUCTION:
            Spinner annoyingMandatorySpinner = (Spinner) findViewById(R.id.annoyingmandatoryspinner);
            // Name of new product
            String pName = itemToAdd.getText().toString();
            // TODO: DELETE SPINNER IMPLEMENTATION FOR PRODUCTION:
            if(prefs.getBoolean("show_qty_field",false))
                pName += ", "+annoyingMandatorySpinner.getSelectedItem().toString();
            // Create new product based on value of input field and add it to bag
            Product newProduct = new Product(pName,auth.getCurrentUser().getDisplayName().toString());
            //bag.add(newProduct);
            ref.push().setValue(newProduct);
            // Empty input field upon submission
            itemToAdd.setText("");
            itemSubmission(adapter);
        }
    }

    // Procedure for submitting items (sorting, then submitting) and showing/hiding the "delete checked" FAB
    private void itemSubmission(FirebaseListAdapter<Product> p){
        //ref.push().getDatabase();
        //p.notifyDataSetChanged();


        // Determine whether or not to show the FAB for deleting products
        boolean display = false;
        for (int i = 0; i < p.getCount(); i++){
            Product prod = p.getItem(i);
            display = (prod.isChecked()) ? true : display;
            Log.d("Display FAB",prod.isChecked()+"");
        }
        Log.d("testDisplay",display+"");
        if(display){
            df.setVisibility(View.VISIBLE);
        } else {
            df.setVisibility(GONE);
        }
        //emptyListMessage();
    }

    // Determine if the message "Your items will appear here..." should be shown, or the list
    /*private void emptyListMessage(){
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
    }*/

    /**
     * Comparator methods:
     */

    // Comparator that sorts the list by checked, unchecked first
    private class uncheckedFirst implements Comparator<Product> {
        @Override
        public int compare(Product p1, Product p2) {
            if(p1.isChecked()) {
                return 1;
            } else return -1;
        }

    }

    // Comparator that sorts the list by date, descending
    private class DateDesc implements Comparator<Product> {
        @Override
        public int compare(Product p1, Product p2) {
            return p1.fetchDate().compareTo(p2.fetchDate());
        }

    }

    /**
     * Dialogs:
     */
    private void showDialog(View v) {
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
        deleteAll();
    }

    /**
     * Snackbar:
     */
    // Snackbar that appears every time something
    // has been deleted, with option to undo
    private void makeSnackbar(int duration){
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
                        findViewById(R.id.fabDelete).setVisibility(GONE); // Hide the 'delete' FAB while snackbar is visible

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
            Intent settingsIntent = new Intent(this,SettingsActivity.class);
            startActivityForResult(settingsIntent,1337);
            return true;
        }

        // Options->Add test products
        if (id == R.id.action_add_test_products) {
            addTestItems();
            return true;
        }

        // Options->Mark all as unseen
        if (id == R.id.action_all_unseen) {
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

        if(id == R.id.action_sharelist){
            if(currentList != null){
                Log.d("List share","Sharing list "+currentList);
                String shareText = getSupportActionBar().getTitle().toString()+"\n";
                shareText += "By "+auth.getCurrentUser().getDisplayName()+"\n";
                shareText += "---------------------\n";
                for (int i = 0; i < adapter.getCount(); i++){
                    Product p = (Product) adapter.getItem(i);
                    shareText += (p.isChecked()) ? "[X] " : "[ ] ";
                    shareText += p.getName()+"\n";
                }
                Log.d("List share",shareText);
                // Setup and start share intent
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
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
    private TimerTask updateProducts = new TimerTask() {
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

                    for (int i = 0; i < adapter.getCount(); i++){
                        Product p = (Product) adapter.getItem(i);
                        if(!p.getSeen() && p.getAuthor() != auth.getCurrentUser().getDisplayName()) {
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
        if(adapter != null){
            for (int i = 0; i < adapter.getCount(); i++){
                bag.clear();
                bag.add((Product)adapter.getItem(i));
            }
            outState.putParcelableArrayList("arraylist", bag);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState){
        super.onRestoreInstanceState(savedState);
        if(adapter != null){
            ListView listView = (ListView) findViewById(R.id.list);
            bag = savedState.getParcelableArrayList("arraylist");
            initAdapters();
            //adapter = adapterInit();

            //setting the adapter on the listview
            //listView.setAdapter(adapter);
            //adapter.notifyDataSetChanged();
            //itemSubmission(adapter);
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if(adapter != null)
            adapter.cleanup();
    }
}
