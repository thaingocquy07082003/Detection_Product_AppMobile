package com.example.pbl5client;

import com.example.pbl5client.Model.Product;
import com.example.pbl5client.Model.ProductResponse;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RetrofitInterface {
    @GET("/image") // Replace with your actual endpoint
    Call<ResponseBody> getImage();

    @GET("/getAll")
    Call <ProductResponse> getAllProduct();

    @PATCH("/thanhToan")
    Call<ResponseBody> thanhToan(@Body List<String> ItemName);

    @POST("/getQR")
    Call<ResponseBody> getQR(@Body List<String> ItemName);
    @POST("/create/sanPham")
    Call<ResponseBody>AddSP(@Body Product product);

    @DELETE("/delete/{id}")
    Call<ResponseBody>deleteSP(@Path("id") Integer id);

}
