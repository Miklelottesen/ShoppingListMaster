package com.junkiesoup.shoppinglist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

public class ListmanActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public EditText listmanEditText;
    public ImageButton listmanAddButton;
    public ListView listmanList;
    private FirebaseListAdapter<ListInstance> listmanAdapter;
    private DatabaseReference listmanRef;
    private FirebaseAuth listmanAuth;
    public Boolean loggedIn = false;

    public Context context;
    public FirebaseAuth auth;

    public ImageView drawerProfilePic;
    public TextView drawerProfileName;
    public TextView drawerProfileEmail;

    private ListInfoAdapter lAdapter;
    private ArrayList<ListInstance> bag;

    private ArrayList<ListInstanceShort> listman_list;
    private String listman_list_string;

    public String uID;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listman_main);

        context = getApplicationContext();
        bag = new ArrayList<>();

        // Get views
        listmanEditText = (EditText) findViewById(R.id.listman_itemInput);
        listmanAddButton = (ImageButton) findViewById(R.id.listmanAddButton);
        listmanList = (ListView) findViewById(R.id.listman_list);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            updateProfileFields();
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

        // Listeners
        listmanAddButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addList();
                    }
                }
        );

        // Android Universal Image Loader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // User stuff
        loggedIn = false;
        /*userEmail = "test@gmail.com";
        userName = "User";
        userPhoto = "";*/

        // Drawer layout
        Toolbar appToolbar = (Toolbar) findViewById(R.id.listman_toolbar);
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

        // Prefs
        prefs = getApplicationContext().getSharedPreferences("appSettings",0);

        ArrayList<ListInstance> li = getIntent().getParcelableArrayListExtra("listinstances");
        Log.d("VIRK",li.toString());
        for(ListInstance l : li){
            Log.d("JAAA?!",l.getName());
        }
        bag.addAll(li);
        lAdapter = new ListInfoAdapter(getApplicationContext(),bag);
        ListView listView = (ListView) findViewById(R.id.listman_list);
        listView.setAdapter(lAdapter);

        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> l, View view, int position, long id) {
                        Log.d("List clicked",bag.get(position).getName());
                        //Intent ma = new Intent(getApplicationContext(),MainActivity.class);
                        //ma.putExtra("listPicked",bag.get(position).getRef());
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result",bag.get(position).getRef());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("currentList",bag.get(position).getRef());
                        editor.commit();
                        setResult(200,returnIntent);
                        finish();
                        //startActivity(ma);
                    }
                }
        );
    }

    public void updateProfileFields(){
        if(loggedIn){
            View v = findViewById(R.id.drawer_profile_pic);
            ImageLoader imageLoader = ImageLoader.getInstance();
            //ImageView dPP = (ImageView) findViewById(R.id.drawer_profile_pic);
            Log.d("WTFF?",drawerProfilePic.toString());
            imageLoader.displayImage(listmanAuth.getCurrentUser().getPhotoUrl()+"", drawerProfilePic);

            drawerProfileName.setText(listmanAuth.getCurrentUser().getDisplayName());
            drawerProfileEmail.setText(listmanAuth.getCurrentUser().getEmail());
        }
    }

    // Adding a new product to the bag, based on value of input field
    private void addList(){
        // !!!!DELETE THIS:
        //int i = 10/0; // WILL make the app crash - just to test crash reporting
        // Get input field
        EditText itemToAdd = (EditText) findViewById(R.id.listman_itemInput);
        if(itemToAdd.getText().toString() != "" && itemToAdd.getText().toString() != null){ // Prevent addition of empty products
            // Create new product based on value of input field and add it to bag
            ListInstance newList = new ListInstance(itemToAdd.getText().toString());
            String addItem = itemToAdd.getText().toString();
            Intent i = new Intent();
            i.putExtra("result",addItem);
            setResult(300,i);
            finish();
        }
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_myLists) {

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
}
