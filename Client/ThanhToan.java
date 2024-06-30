package com.example.pbl5client;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.pbl5client.Model.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ThanhToan extends AppCompatActivity {
    OrderedProductArrayAdapter myArrayAdapter;
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private String BASE_URL = "http://10.0.2.2:8080/";
    private ImageView qrCodeImageView;

    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanh_toan);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        ArrayList<Product> products = new ArrayList<Product>();
        products = (ArrayList<Product>) getIntent().getSerializableExtra("orderedList");
        System.out.println("size :" + products.size());

//        lv = findViewById(R.id.GetAllListView);
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofitInterface = retrofit.create(RetrofitInterface.class);
//        myArrayAdapter = new OrderedProductArrayAdapter(ThanhToan.this,R.layout.ordered_product_item,products);
//        lv.setAdapter(myArrayAdapter);
        Button ThanhToan = (Button) findViewById(R.id.thanh_toan_btn);
        ArrayList<Product> finalProducts = products;


        List<String> itemName= new ArrayList<String>();
        for(Product p : products) {
            itemName.add(p.getName());
        }
        Call<ResponseBody> call = retrofitInterface.getQR(itemName);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        // Lấy ảnh QR từ phản hồi
                        byte[] qrCodeBytes = response.body().bytes();
                        String qrCodeBase64 = Base64.encodeToString(qrCodeBytes, Base64.DEFAULT);

                        // Hiển thị ảnh QR trên ImageView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                qrCodeImageView.setVisibility(View.VISIBLE);
                                qrCodeImageView.setImageBitmap(
                                        BitmapFactory.decodeByteArray(qrCodeBytes, 0, qrCodeBytes.length));
                            }
                        });

                        System.out.println("Response from server: " + qrCodeBase64);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Server returned an error: " + response.errorBody().toString());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("Failed to make request: " + t.getMessage());
            }
        });



        ThanhToan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> itemName= new ArrayList<String>();
                for(Product p : finalProducts) {
                     itemName.add(p.getName());
                }
                Call<ResponseBody> call = retrofitInterface.thanhToan(itemName);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try {
                                // In ra phản hồi từ server
                                String res = response.body().string();
                                System.out.println("Response from server: " + res);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Server returned an error: " + response.errorBody().toString());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });
            }
        });
    }

 }