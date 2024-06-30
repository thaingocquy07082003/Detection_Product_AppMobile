package com.example.pbl5client;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pbl5client.Model.Product;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderedProductArrayAdapter extends ArrayAdapter<Product> implements Filterable {
    Activity context;
    int IdLayout;
    ArrayList<Product> myList;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private String BASE_URL = "http://10.0.2.2:3000/";

    public OrderedProductArrayAdapter(Activity context, int IdLayout, ArrayList<Product> myList) {
        super(context, IdLayout,myList);
        this.context = context;
        this.IdLayout = IdLayout;
        this.myList = myList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater myInflater = context.getLayoutInflater();
        convertView = myInflater.inflate(IdLayout,null);
        Product product = myList.get(position);

        TextView id = convertView.findViewById(R.id.Mssv);
        id.setText(product.getId().toString());

        TextView name = convertView.findViewById(R.id.TenSV);
        name.setText(product.getName().toString());

        TextView lop = convertView.findViewById(R.id.Lop);
        lop.setText(product.getQuantity().toString());


        return  convertView;
    }
}
