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

import java.util.ArrayList;

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
        productDate.setText(product.getDate());

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
}
