package com.example.v_sat_compass.ui.collaborator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class QuestionEditorPagerAdapter extends FragmentStateAdapter {

    public QuestionEditorPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1: return QuestionAnswerFragment.newInstance();
            case 2: return QuestionSettingsFragment.newInstance();
            default: return QuestionContentFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() { return 3; }
}
