package com.junkiesoup.shoppinglist;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Mikkel on 15-09-2016.
 */
public class Product implements Parcelable {
    private String name;
    private int userID;
    private Date added;
    private Date modified;
    //private String date;
    private boolean checked;
    private boolean seen;
    public int position;


    public Product(){}

    // Constructor for before users are added - will assign userID 0
    public Product(String name){
        this.name = name;
        this.userID = 1;
        this.checked = false;
        this.seen = false;

        // Get current date
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Date currentLocalTime = cal.getTime();

        // Assign dates
        this.added = currentLocalTime;
        this.modified = this.added;
        //this.date = setDateDisplay(currentLocalTime);
    }

    // TEST ONLY: allows to set date
    public Product(String name, Date date){
        this.name = name;
        this.userID = 2;
        this.checked = false;
        this.seen = false;

        // Assign dates
        this.added = date;
        this.modified = this.added;
        //this.date = setDateDisplay(date);
    }

    // Parcelable implementation methods
    @Override
    public int describeContents(){
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        //dest.writeString(date);
        dest.writeInt(userID);
        dest.writeSerializable(added);
        dest.writeSerializable(modified);
        dest.writeInt(seen ? 1 : 0);
        dest.writeInt(checked ? 1 : 0);
        //if(seen) dest.writeInt(1); else dest.writeInt(0);
        //if(checked) dest.writeInt(1); else dest.writeInt(0);
    }
    // Creator
    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    // "De-parcel object
    public Product(Parcel in) {
        name = in.readString();
        //date = in.readString();
        userID = in.readInt();
        added = (Date) in.readSerializable();
        modified = (Date) in.readSerializable();
        seen = (in.readInt()==1) ? true : false;
        checked = (in.readInt()==1) ? true : false;
    }

    /**
     * Methods:
     */

    /*public void updateDisplayDate(){
        this.date = setDateDisplay(this.modified);
    }*/

    // Return a string with date info (to display on the product cards)
    /*private String setDateDisplay(Date d){
        String returnString = MainActivity.resources.getString(R.string.date_not_found);

        // Get current date
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Date clt = cal.getTime(); // Current local time


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
    }*/
    public String getName(){
        return this.name;
    }
    public String getDate(){
        return this.modified.toString();
    }
    public Date fetchDate() { return this.modified; }
    public int getAuthor(){
        return this.userID;
    }
    public boolean getSeen(){
        return this.seen;
    }
    public boolean isChecked(){
        return (this.checked) ? true : false;
    }
    public void setChecked(){
        if(this.checked) {
            this.checked = false;
        } else {
            this.checked = true;
        }
    }
    public void setSeen(boolean s){
        this.seen = s;
    }

    // TEST METHODS
    public String consoleLog(){
        return "Product added: "+this.name+"\n"+
                "Added: "+this.modified.toString()+"\n"+
                "User: "+this.userID+"\n"+
                "Is checked: "+this.checked;
    }
    // Function for returning date values as int
    /*public int di (DateFormat f, Date D){
        String s = f.format(D);
        int r = Integer.parseInt(s);
        return r;
    }*/
}
