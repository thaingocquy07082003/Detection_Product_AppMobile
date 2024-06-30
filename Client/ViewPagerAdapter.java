package com.example.pbl5client;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;



public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new Tab1(); // Fragment cho tab 1
            case 1:
                return new Tab2(); // Fragment cho tab 2
            default:
                return new Tab1();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Số lượng tab
    }
}
