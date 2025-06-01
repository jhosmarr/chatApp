package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class TabsAccessorAdapter extends FragmentPagerAdapter {

    public TabsAccessorAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            // agregando return y un default por cualquier cosa
            case 0:
                return new ChatsFragment();
            case 1:
                return new GroupsFragment();
            case 2:
                return new ContactsFragment();
            case 3:
                return new RequestsFragment();
            default:
                return new ChatsFragment();
        }
    }



    @Override
    public int getCount()
    {
        return 4; // Total number of tabs
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "CHATS";
            case 1:
                return "GROUPS";
            case 2:
                return "CONTACTS";
            case 3:
                return "Requests";
            default:
                return null;
        }
    }
}
