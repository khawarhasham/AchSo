package fi.aalto.legroup.achso.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import javax.annotation.Nonnull;

import fi.aalto.legroup.achso.util.FloatPosition;

/**
 * An area for markers that can be positioned, selected, and dragged around.
 *
 * @author Leo Nikkilä
 */
public class MarkerCanvas extends FrameLayout implements View.OnClickListener {

    private Listener listener;

    public MarkerCanvas(Context context) {
        super(context);
    }

    public MarkerCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarkerCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Marker addMarker(FloatPosition position, Drawable background) {
        return addMarker(position, background, true);
    }

    public Marker addMarker(FloatPosition position, Drawable background, boolean isDraggable) {
        Marker marker = new Marker(getContext());

        marker.setBackground(background);
        marker.setDraggable(isDraggable);
        marker.setOnClickListener(this);

        addView(marker);

        float posX = getWidth() * position.getX() - marker.getWidth() / 2;
        float posY = getHeight() * position.getY() - marker.getHeight() / 2;

        marker.setX(posX);
        marker.setY(posY);

        return marker;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // Compute new marker positions
        for (int i = 0; i < getChildCount(); i++) {
            Marker marker = (Marker) getChildAt(i);

            float newPosX = marker.getX() / oldWidth * width;
            float newPosY = marker.getY() / oldHeight * height;

            marker.setX(newPosX);
            marker.setY(newPosY);
        }
    }

    public void removeMarker(Marker marker) {
        removeView(marker);
    }

    public void clearMarkers() {
        removeAllViews();
    }

    private void canvasTapped(MotionEvent event) {
        if (listener == null) return;

        float posX = event.getX() / getWidth();
        float posY = event.getY() / getHeight();

        FloatPosition position = new FloatPosition(posX, posY);

        listener.onCanvasTapped(position);
    }

    /**
     * Called when a marker is tapped.
     */
    @Override
    public void onClick(View view) {
        if (listener != null) listener.onMarkerTapped((Marker) view);
    }

    /**
     * Called when the canvas is touched.
     */
    @Override
    public boolean onTouchEvent(@Nonnull MotionEvent event) {
        int action = event.getActionMasked();

        // When the canvas is tapped, clear the selection if we have one. Otherwise send a canvas
        // tap event.
        if (action == MotionEvent.ACTION_DOWN) {
            canvasTapped(event);
        }

        return super.onTouchEvent(event);
    }

    /**
     * Accepts dragged markers that are dropped onto the container and sets their new position.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        int action = event.getAction();

        if (action == DragEvent.ACTION_DROP) {
            Marker marker = (Marker) event.getLocalState();

            Float posX = event.getX();
            Float posY = event.getY();

            marker.setX(posX - marker.getWidth() / 2f);
            marker.setY(posY - marker.getHeight() / 2f);

            Float relativeX = posX / getWidth();
            Float relativeY = posY / getHeight();

            FloatPosition pos = new FloatPosition(relativeX, relativeY);

            listener.onMarkerDragged(marker, pos);
        }

        return true;
    }

    public interface Listener {

        public void onMarkerTapped(Marker marker);

        public void onMarkerDragged(Marker marker, FloatPosition newPos);

        public void onCanvasTapped(FloatPosition pos);

    }

}
