package com.example.pbl5client;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

import com.example.pbl5client.Model.Product;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class Tab2 extends Fragment {

    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private FrameLayout frameLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String BASE_URL = "http://10.0.2.2:8080/";
    private static final long REQUEST_INTERVAL = 5000; // Interval in milliseconds
    private boolean isRequesting = true; // Flag to control request sending
    private ArrayList<Product> orderedList = new ArrayList<Product>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab2, container, false);
        frameLayout = view.findViewById(R.id.Img); // Initialize frameLayout

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofitInterface = retrofit.create(RetrofitInterface.class);

        // Start sending requests periodically
        startSendingRequests(view);

        Button butoton =view.findViewById(R.id.tiep_theo);
        butoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ThanhToan.class);
                intent.putExtra("orderedList" , orderedList);
                startActivity(intent);

            }
        });

        return view;
    }

    private void startSendingRequests(View view) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRequesting) {
                    sendRequest(view); // Send request to server
                    handler.postDelayed(this, REQUEST_INTERVAL); // Send request periodically
                }
            }
        }, REQUEST_INTERVAL);
    }

    private void sendRequest(View view) {
        Call<ResponseBody> call = retrofitInterface.getImage();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {

                    try {
                        String responseData = response.body().string();
                        Log.d("Response", "Data from server: " + responseData);

                        // Parse JSON response
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject productJson = jsonObject.getJSONObject("product");

                        String productName = productJson.getString("name");
                        int productQuantity = productJson.getInt("quantity");
                        int productPrice = productJson.getInt("price");

                        // Update UI components
                        TextView id = view.findViewById(R.id.id);
                        TextView name = view.findViewById(R.id.Ten);
                        TextView quantity = view.findViewById(R.id.SL);

                        id.setText(String.valueOf(productJson.getInt("p_id")));
                        name.setText(productName);
                        quantity.setText(String.valueOf(productQuantity));

                        // Update image (assuming the image URL is in the productJson)
                        // This part needs to be updated according to how the image is provided.
                        // For example, if it's a URL, you might want to use an image loading library like Glide.
                        // Here we're setting a placeholder as an example.
                        if (productName.equalsIgnoreCase("Coca")) {
                            Drawable drawable = getResources().getDrawable(R.drawable.coca, null);
                            frameLayout.setBackground(drawable);
                        } else {
                            // Handle other cases
                        }

                        // Add product to the ordered list
                        Product product = new Product(productJson.getInt("p_id"), productName, productQuantity, productPrice);
                        orderedList.add(product);


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }



                } else {
                    Log.e("Response", "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Request", "Failed to send request: " + t.getMessage());
            }
        });
    }
}
