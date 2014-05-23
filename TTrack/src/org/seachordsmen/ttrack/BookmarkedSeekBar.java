package org.seachordsmen.ttrack;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.SeekBar;

@SuppressLint("UseSparseArrays")
public class BookmarkedSeekBar extends SeekBar {
    private static final Logger LOG = LoggerFactory.getLogger(BookmarkedSeekBar.class);

    private class Bookmark {
        Bookmark(int pos, int color) {
            this.pos = pos;
            this.paint = new Paint(getResources().getColor(color));
            computeRect();
        }
        int pos;
        Paint paint;
        Rect rect;
        void computeRect() {
            int cw = getWidth() - getPaddingLeft() - getPaddingRight();
            double relPos = (double)pos / getMax();
            int pos = (int)(relPos * cw) + getPaddingLeft();
            int dy = getHeight() / 4;
            rect = new Rect(pos-1, dy, pos+1, 3*dy);
            //LOG.debug("Rect: " + rect.flattenToString());
        }
    }
    
    private Map<Integer, Bookmark> bookmarks = new HashMap<Integer, Bookmark>();
	
	public BookmarkedSeekBar(Context context) {
		super(context);
	}
	
	public BookmarkedSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BookmarkedSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for (Bookmark bookmark : bookmarks.values()) {
		    canvas.drawRect(bookmark.rect, bookmark.paint);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		//LOG.debug("onLayout");
		super.onLayout(changed, left, top, right, bottom);
		computeRects();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		computeRects();
	}

	public void setBookmark(int id, int pos, int color) {
	    if (pos >= 0) {
	        bookmarks.put(id, new Bookmark(pos, color));
	    } else {
	        bookmarks.remove(id);
	    }
	}

	private void computeRects() {
	    for (Bookmark bookmark : bookmarks.values()) {
	        bookmark.computeRect();
	    }
		invalidate();
	}

}
