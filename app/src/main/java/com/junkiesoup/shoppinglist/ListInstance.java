package com.junkiesoup.shoppinglist;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mikkel on 11/24/16.
 */

public class ListInstance implements Parcelable {
    private String name;
    private ArrayList<String> users;
    private ArrayList<Product> items;
    private Long date;
    private String ref;

    public ListInstance(){}
    public ListInstance(String listName){
        this.name = listName;
        this.users = new ArrayList<String>();
        this.items = new ArrayList<Product>();
        this.date = new Date().getTime();
        this.ref = null;
    }
    public ListInstance(String name, ArrayList<String> users, ArrayList<Product> items, Long date, String ref){
        this.name = name;
        this.users = users;
        this.items = items;
        this.date = date;
        this.ref = ref;
    }
    public void addUser(String userID){
        this.users.add(userID);
    }

    public int removeUser(String userID){
        for(int i = 0; i < this.users.size(); i++){
            if(this.users.get(i) == userID){
                this.users.remove(i);
            }
        }
        return this.users.size();
    }
    public void updateDate(Date newDate){
        this.date = newDate.getTime();
    }
    public void updateDate(){
        updateDate(new Date());
    }
    public String getName(){
        return this.name;
    }
    public void setName(String name) { this.name = name;}
    public Long getDate() { return this.date; }
    public void setDate(Long date) { this.date = date;}
    public int countUsers() {
        return (this.users != null) ? this.users.size() : 0;
    }
    public ArrayList<Product> getItems(){
        return (this.items != null) ? this.items : new ArrayList<Product>();
    }
    public void setRef(String ref){
        this.ref = ref;
    }
    @Nullable
    public String getRef(){
        return this.ref;
    }
    public ArrayList<String> getUsers(){ return this.users; }
    public void setUsers(String uID){
        if(this.users == null) this.users = new ArrayList<String>();
        this.users.add(uID);
    }
    public void setItems(String itemName, String userName){
        if (this.items == null) this.items = new ArrayList<Product>();
        this.items.add(new Product(itemName, userName));
    }

    // Parcelable implementation methods
    @Override
    public int describeContents(){
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeList(users);
        dest.writeList(items);
        dest.writeLong(date);
        dest.writeString(ref);
    }
    // Creator
    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public ListInstance createFromParcel(Parcel in) {
            return new ListInstance(in);
        }

        public ListInstance[] newArray(int size) {
            return new ListInstance[size];
        }
    };

    // "De-parcel object
    public ListInstance(Parcel in) {
        name = in.readString();
        users = new ArrayList<String>();
        in.readList(users,null);
        items = new ArrayList<Product>();
        in.readList(items,null);
        date = in.readLong();
        ref = in.readString();
    }
}
