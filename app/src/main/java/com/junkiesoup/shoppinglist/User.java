package com.junkiesoup.shoppinglist;

/**
 * Created by Mikkel on 21-09-2016.
 */
public class User {
    private String name;
    private int id;

    public User (String name, int id){
        this.name = name;
        this.id = id;
    }

    // Methods
    public int getId (){
        return this.id;
    }
    public String getName (){
        return this.name;
    }
}
