package toptierlabs.sampleapp;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Extends Relative layout to support dragging of
 */

public class TopicsDragLayout extends RelativeLayout {

    private static final String MTAG = "TopicsDragLayout";

    private int mDraggingState = 0; // the dragging status
    private int mDraggingBorder; // tracking the top border of the dragged view
    private int mVerticalRange;  // TODO: what does it measure???????????

    private ViewDragHelper mDragHelper;

    public class DragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public void onViewDragStateChanged(int state) {
            if (state == mDraggingState) // no change
                return;

//            if ((mDraggingState == ViewDragHelper.STATE_DRAGGING ||
//                    mDraggingState == ViewDragHelper.STATE_SETTLING)
//                    && state == ViewDragHelper.STATE_IDLE) {
//                // the view stopped from moving.
//
//                // this is used to check if the view dragged got closed or opened
//                if (mDraggingBorder == mVerticalRange)
//                    mIsOpen = true;
//            }

//            if (state == ViewDragHelper.STATE_DRAGGING) {
//                onStartDragging();
//            }

            Log.v(MTAG, "drag state: " + state);
            mDraggingState = state;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mDraggingBorder = top;
        }

        /**
         * is a view enabled for dragging?
         */
        @Override
        public boolean tryCaptureView(View view, int i) {
            return (view.getId() == R.id.topic_fragment_container);
        }

        /**
         * Restricts the movement of a view vertically
         */
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
//            final int topBound = getPaddingTop();
//            final int bottomBound = mVerticalRange;
//
//            return Math.min(Math.max(top, topBound), bottomBound);
            Log.v(MTAG, "clamp view method: top trying to go: " + top);

            return top;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final double AUTO_OPEN_SPEED_LIMIT = 800.0;

            final float rangeToCheck = mVerticalRange;
            if (mDraggingBorder == 0) {
//                mIsOpen = false;
                return;
            }
            if (mDraggingBorder == rangeToCheck) {
//                mIsOpen = true;
                return;
            }
            boolean settleToOpen = false;
            if (yvel > AUTO_OPEN_SPEED_LIMIT) { // speed has priority over position
                settleToOpen = true;
            } else if (yvel < -AUTO_OPEN_SPEED_LIMIT) {
                settleToOpen = false;
            } else if (mDraggingBorder > rangeToCheck / 2) {
                settleToOpen = true;
            } else if (mDraggingBorder < rangeToCheck / 2) {
                settleToOpen = false;
            }

            final int settleDestY = settleToOpen ? mVerticalRange : 0;

            if(mDragHelper.settleCapturedViewAt(0, settleDestY)) {
                ViewCompat.postInvalidateOnAnimation(TopicsDragLayout.this);
            }
        }
    }

    public TopicsDragLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        // TODO: initialize the drag for topics view
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mVerticalRange = (int) (h * 0.66);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onFinishInflate() {
        Log.v(MTAG, "Topics drag inflating");
        mDragHelper = ViewDragHelper.create(this, 1.0f, new DragHelperCallback());
        super.onFinishInflate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(MTAG, "touch event");

        mDragHelper.processTouchEvent(event);
        return true;
//        if (isQueenTarget(event) || isMoving()) {
//            mDragHelper.processTouchEvent(event);
//            return true;
//        } else {
//            return super.onTouchEvent(event);
//        }
    }

    @Override
    public void computeScroll() { // needed for automatic settling.
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

}
