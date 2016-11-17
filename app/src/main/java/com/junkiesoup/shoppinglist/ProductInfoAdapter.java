package com.junkiesoup.shoppinglist;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.junkiesoup.shoppinglist.Product;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Mikkel on 15-09-2016.
 */
public class ProductInfoAdapter extends ArrayAdapter<Product> {
    public Context context;
    public ArrayList<User> users = new ArrayList<User>();
    public ProductInfoAdapter(Context context, ArrayList<Product> products) {
        super(context, 0, products);
        this.context = context;  /*Context context is defined in the PlayerInfoAdapter class as a global variable to save the context for later use.*/
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final Product product = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.product_card, parent, false);
        }
        //TextView productInfoView1 = (TextView) convertView.findViewById(R.id.playerInfoText1);
        TextView productAuthor = (TextView) convertView.findViewById(R.id.user);

        // Get the username
            // Create test users
        users.add(new User("Mikkel",1));
        users.add(new User("Tabitha",2));
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

        return convertView;
    }
    // Method for creating a user friendly date string, based on a Date object
    private String setDateDisplay(Date d){
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
}
