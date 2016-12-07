package com.junkiesoup.shoppinglist;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Mikkel on 15-09-2016.
 */
public class Utils{
    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    public FirebaseRemoteConfigSettings firebaseRemoteConfigSettings(){
        return new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)  //set to false when releasing
                .build();
    }
    // Method for creating a user friendly date string, based on a Date object
    public String setDateDisplay(Long date){
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
    private int di (DateFormat f, Date D){
        String s = f.format(D);
        int r = Integer.parseInt(s);
        return r;
    }
}
