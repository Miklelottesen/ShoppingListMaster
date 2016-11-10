package com.junkiesoup.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;

import android.support.design.widget.Snackbar;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ConfirmDeleteDialogFragment.OnPositiveListener {
    // For the delete dialog
    static ConfirmDeleteDialogFragment dialog;
    public Context context;

    public static Resources resources; // Declare resources object, to be used when accessing string resources in other classes
    public int productToEdit; // For deleting through context menu - will store the bag position of the item to delete

    // Declare adapter, listView, bag and df (the "delete checked" FAB)
    public ProductInfoAdapter adapter;
    public ListView listView;
    static ArrayList<Product> bag = new ArrayList<>();
    static ArrayList<Product> backupBag = new ArrayList<>();
    FloatingActionButton df;

    // Boolean to momentarily disable the timer
    public boolean timerUpdateEnabled = true;

    int currentUser = 1; // TEST: sets ID for current user

    //AlertDialog.Builder builder = new AlertDialog.Builder(this);

    // Function for getting the adapter
    public ArrayAdapter getMyAdapter()
    {
        return adapter;
    }

    ArrayList<Product> postDeleteList;

    // Animation
    Animation animFadeOut;
    Animation animFadeOutDelete;

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

        postDeleteList = new ArrayList<>();

        //builder.setView(findViewById(R.id.edit_product_view));

        // Get resources for use in other classes
        resources = getResources();

        // Load animations
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
        adapter = new ProductInfoAdapter(this, bag); // Custom adapter

        //setting the adapter on the listview
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // Listener for clicking on a list item
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> l, View view, int position, long id) {
                        view.startAnimation(animFadeOut);
                        // Get listitem in question, set it to "checked" and submit to the adapter
                        Product i = bag.get(position);
                        i.setChecked();
                        // itemsubmissionadapter
                    }
                }
        );

        listView.setOnTouchListener(
                new OnSwipeTouchListener(MainActivity.this){
                    public void onSwipeRight() {
                        Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
                    }
                    public void onSwipeLeft() {
                        Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                    }
                }
        );

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
    } /** onCreate end */

    /**
     * Methods:
     */

    public void continueDeletion(){
        bag.clear();
        adapter.notifyDataSetChanged();
        bag.addAll(postDeleteList);
        itemSubmission(adapter);
        makeSnackbar(5000);
    }

    // TEST: Add a bunch of test items to the bag (call this function in onCreate, AFTER initializing bag and adapter)
    public void addTestItems(){
        bag.add(new Product("Køkkenrulle",new Date(2016-1900,9-1,17,14,10)));
        bag.add(new Product("Fryseposer",new Date(2016-1900,9-1,17,14,11)));
        bag.add(new Product("Afkalker",new Date(2015-1900,11-1,11,14,10)));
        bag.add(new Product("Hørfrø",new Date(2016-1900,11-1,2,14,10)));
        bag.add(new Product("Dadler",new Date(2016-1900,8-1,11,14,10)));
        bag.add(new Product("4 appelsiner",new Date(2016-1900,9-1,11,14,10)));
        bag.add(new Product("Sukker",new Date(2016-1900,9-1,21,9,10)));
        bag.add(new Product("Mel",new Date(2016-1900,9-1,21,15,10)));
        bag.add(new Product("Pistols"));
        itemSubmission(adapter);
    }

    // Adding a new product to the bag, based on value of input field
    public void addItem(){
        // Get input field
        EditText itemToAdd = (EditText) findViewById(R.id.itemInput);
        // Create new product based on value of input field and add it to bag
        Product newProduct = new Product(itemToAdd.getText().toString());
        bag.add(newProduct);
        // Empty input field upon submission
        itemToAdd.setText("");
        itemSubmission(adapter);
    }

    // Procedure for submitting items (sorting, then submitting) and showing/hiding the "delete checked" FAB
    public void itemSubmission(ProductInfoAdapter p){
        p.sort(new DateDesc());
        p.sort(new uncheckedFirst());
        p.notifyDataSetChanged();

        // Determine whether or not to show the FAB for deleting products
        boolean display = false;
        for (Product prod : bag) {
            display = (prod.isChecked()) || display;
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

        // Animated transition between empty list message and listview
        if(getMyAdapter().isEmpty() && !textView.isShown()) {
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
        else if(!getMyAdapter().isEmpty() && textView.isShown()){
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
        backupBag.clear();
        backupBag.addAll(bag);
        adapter.clear();
        bag.clear();
        itemSubmission(adapter);
        makeSnackbar(6000);
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
        int delCount = backupBag.size(); // Number of deleted items
        String m = delCount+" "+((delCount == 1) // Make string with delCount and string values
                ? getString(R.string.snackbar_text_single)
                : getString(R.string.snackbar_text_plural));

        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.mainLayout), m, duration)
                .setAction(getString(R.string.snackbar_undo), new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        bag.addAll(backupBag);
                        backupBag.clear();
                        itemSubmission(adapter);
                        Log.d("Snackbar","Snackbar came!");
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
            ArrayList<Product> tempBag = new ArrayList<Product>();
            for (Product p : bag) {
                p.setSeen(false);
                tempBag.add(p);
            }
            bag.clear();
            bag.addAll(tempBag);
            itemSubmission(adapter);
            return true;
        }

        // Options->Mark all as seen
        if (id == R.id.action_all_seen) {
            ArrayList<Product> tempBag = new ArrayList<Product>();
            for (Product p : bag) {
                p.setSeen(true);
                tempBag.add(p);
            }
            bag.clear();
            bag.addAll(tempBag);
            itemSubmission(adapter);
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
                    ArrayList<Product> tempBag = new ArrayList<Product>();
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
        outState.putParcelableArrayList("arraylist", bag);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState){
        super.onRestoreInstanceState(savedState);
        ListView listView = (ListView) findViewById(R.id.list);
        bag = savedState.getParcelableArrayList("arraylist");
        adapter = new ProductInfoAdapter(this, bag);

        //setting the adapter on the listview
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        itemSubmission(adapter);
    }

}
