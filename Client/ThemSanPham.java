package com.example.pbl5client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pbl5client.Model.Product;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ThemSanPham extends AppCompatActivity {

    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private SearchView searchView;
    private String BASE_URL = "http://10.0.2.2:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_san_pham);
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofitInterface = retrofit.create(RetrofitInterface.class);
        Button addSP = (Button) findViewById(R.id.AddSP_Them);
        addSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText Id = findViewById(R.id.AddSP_Id);
                final EditText TenSP = findViewById(R.id.AddSP_Name);
                final EditText GiaSP = findViewById(R.id.AddSP_Gía);
                final EditText SL = findViewById(R.id.AddSP_SoLuong);
                final EditText AnhSP = findViewById(R.id.AddSP_AnhSP);

                Product product = new Product(
                        Integer.parseInt(Id.getText().toString()),
                        TenSP.getText().toString(),
                        Integer.parseInt(SL.getText().toString()) ,
                        Integer.parseInt(GiaSP.getText().toString())
                );
                Call<ResponseBody> call = retrofitInterface.AddSP(product);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            // Xử lý khi yêu cầu thành công

//                            String message = response.body().toString();
//                            Toast.makeText(ThemSanPham.this, message, Toast.LENGTH_LONG).show();
//                            System.out.println("da tao duoc san pham");
                        } else {
//                            String errorMessage = response.errorBody().toString();
//                            Toast.makeText(ThemSanPham.this, errorMessage, Toast.LENGTH_LONG).show();
//                            Log.e("Error", errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("Error", t.getMessage());
                    }
                });
            }
        });
    }

}