package com.googlii.activities;

import com.parse.ui.ParseLoginDispatchActivity;

/**
 * Created by niravsaraiya on 10/10/15.
 */
public class MainTabDispatchActivity extends ParseLoginDispatchActivity {
    @Override
    protected Class<?> getTargetClass() {
        return TabsHeaderActivity.class;
    }
}