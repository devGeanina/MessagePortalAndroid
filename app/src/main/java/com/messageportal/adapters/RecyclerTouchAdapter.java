package com.messageportal.adapters;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

import com.messageportal.interfaces.RecyclerClickHandler;


public class RecyclerTouchAdapter implements RecyclerView.OnItemTouchListener{

    private GestureDetector gestureDetector;
    private RecyclerClickHandler recyclerTouchAdapter;

    public RecyclerTouchAdapter(Context context, final RecyclerView recyclerView, final RecyclerClickHandler recyclerAdapter) {
        this.recyclerTouchAdapter = recyclerAdapter;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null && recyclerTouchAdapter != null) {
                    recyclerTouchAdapter.onLongClick(child, recyclerView.getChildPosition(child));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child = rv.findChildViewUnder(e.getX(), e.getY());
        if (child != null && recyclerTouchAdapter != null && gestureDetector.onTouchEvent(e)) {
            recyclerTouchAdapter.onClick(child, rv.getChildPosition(child));
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}
