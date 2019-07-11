package de.htwdd.htwdresden;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import de.htwdd.htwdresden.adapter.ViewPagerAdapter;
import de.htwdd.htwdresden.classes.Const;
import de.htwdd.htwdresden.types.TabItem;


/**
 * Übersicht über die verschiedenen Mensa-Funktionalitäten
 *
 * @author Kay Förster
 */
public class MensaFragment extends Fragment {
    private List<TabItem> mTabs = new ArrayList<>();

    public MensaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle_1 = new Bundle();
        bundle_1.putInt(Const.BundleParams.CANTEEN_DETAIL_MODE, 0);
        final Bundle bundle_2 = new Bundle();
        bundle_2.putInt(Const.BundleParams.CANTEEN_DETAIL_MODE, 1);
        mTabs.add(new TabItem(
                getResources().getString(R.string.mensa_tab_today),
                MensaDetailListFragment.class,
                bundle_1
        ));
        mTabs.add(new TabItem(
                "Karte",
                MensaDetailWeekFragment.class,
                bundle_2
        ));
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_tabs, container, false);
        final ViewPager viewPager = view.findViewById(R.id.viewpager);

        // Adapter für Tabs erstellen und an view hängen
        viewPager.setAdapter(new ViewPagerAdapter(getChildFragmentManager(), mTabs));

        // TabLayout "stylen"
        final TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);
        // Setze feste Anzahl an Tabs
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        // Tabs nehmen immer die ganze Breite ein
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}
