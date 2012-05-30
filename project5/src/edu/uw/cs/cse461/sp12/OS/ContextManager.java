package edu.uw.cs.cse461.sp12.OS;

import android.content.Context;
public class ContextManager {
    private static Context mContext;
    
    public static void setContext(Context context) {
        mContext = context;
    }
    public static Context getContext() {
        return mContext;
    }
    
}
