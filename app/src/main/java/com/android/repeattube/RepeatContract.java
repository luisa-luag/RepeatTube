package com.android.repeattube;

import android.provider.BaseColumns;

/**
 * Created by otavi on 11/24/2017.
 */

/**
 * Contract for DB
 */
public class RepeatContract {

    private RepeatContract(){}

    public static class RepeatEntry implements BaseColumns {
        static final String TABLE_NAME = "repeatCount";
        static final String COLUMN_VIDEO_ID = "videoID";
        static final String COLUMN_REPEAT_COUNT = "repeats";
    }
}
