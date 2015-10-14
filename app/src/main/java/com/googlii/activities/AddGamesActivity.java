package com.googlii.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.googlii.R;
import com.googlii.adapters.ScheduleRecyclerAdapter;
import com.googlii.utils.MyRecyclerScroll;
import com.googlii.utils.Utils;


public class AddGamesActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView recyclerView;
    int fabMargin;
    LinearLayout toolbarContainer;
    int toolbarHeight;
    FrameLayout fab;
    ImageButton fabBtn;
    View fabShadow;
//    boolean fadeToolbar = false;
    ScheduleRecyclerAdapter scheduleRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.simple_grow);

        setContentView(R.layout.activity_add_games);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        //  FAB margin needed for animation
        fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);
        toolbarHeight = Utils.getToolbarHeight(this);

        toolbarContainer = (LinearLayout) findViewById(R.id.fabhide_toolbar_container);
        recyclerView = (RecyclerView) findViewById
                (R.id.recyclerview);

        /* Set top padding= toolbar height.
         So there is no overlap when the toolbar hides.
         Avoid using 0 for the other parameters as it resets padding set via XML!*/
        recyclerView.setPadding(recyclerView.getPaddingLeft(), toolbarHeight,
                recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        if (scheduleRecyclerAdapter == null) {
            scheduleRecyclerAdapter = new ScheduleRecyclerAdapter(this);
            recyclerView.setAdapter(scheduleRecyclerAdapter);
        }

        recyclerView.addOnScrollListener(new MyRecyclerScroll() {
            @Override
            public void show() {
                toolbarContainer.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
//                if (fadeToolbar)
                toolbarContainer.animate().alpha(1).setInterpolator(new DecelerateInterpolator(1)).start();
                fab.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            }

            @Override
            public void hide() {
                toolbarContainer.animate().translationY(-toolbarHeight).setInterpolator(new AccelerateInterpolator(2)).start();
//                if (fadeToolbar)
                toolbarContainer.animate().alpha(0).setInterpolator(new AccelerateInterpolator(1)).start();
                fab.animate().translationY(fab.getHeight() + fabMargin).setInterpolator(new AccelerateInterpolator(2)).start();
            }
        });

        fab = (FrameLayout) findViewById(R.id.myfab_main);
        fabBtn = (ImageButton) findViewById(R.id.myfab_main_btn);
        fabShadow = findViewById(R.id.myfab_shadow);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabShadow.setVisibility(View.GONE);
            fabBtn.setBackground(getDrawable(R.drawable.ripple_accent));
        }

        fab.startAnimation(animation);

        fabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleRecyclerAdapter.saveSelectedMatches();
            }
        });

    }
}
