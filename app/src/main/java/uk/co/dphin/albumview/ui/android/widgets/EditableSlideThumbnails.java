package uk.co.dphin.albumview.ui.android.widgets;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.view.DragEvent;
import android.view.View;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.models.Slide;

public class EditableSlideThumbnails extends HorizontalSlideThumbnails {

    public EditableSlideThumbnails(Context c) {
        super(c);
    }

    public View makeThumbnail(Slide slide, int viewId)
    {
        View thumbnail = super.makeThumbnail(slide, viewId);
        makeThumbnailDraggable(thumbnail);
        return thumbnail;
    }

    private void makeThumbnailDraggable(View thumbnail)
    {
        thumbnail.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO: Is it better to represent the dragged slide as the slide itself, or its number?
                Slide draggedSlide = getAlbum().getSlides().get(v.getId());
                ClipData.Item item = new ClipData.Item(new Integer(v.getId()).toString());
                ClipData dragData = ClipData.newPlainText(item.getText(), item.getText());
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                v.startDrag(dragData, shadow, draggedSlide, 0);

                return true;
            }
        });
        thumbnail.setOnDragListener(this);
        // Drag over the sort options to scroll left
        View sortOptions = ((Activity)getContext()).findViewById(R.id.sortOptions);
        sortOptions.setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_LOCATION:
                        EditableSlideThumbnails.this.scrollBy((10*-1), 0);
                }
                return true;
            }
        });

        // Drag over the add options to scroll right
        View addOptions = ((Activity)getContext()).findViewById(R.id.addOptions);
        addOptions.setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction()) {
                    case DragEvent.ACTION_DRAG_LOCATION:
                        EditableSlideThumbnails.this.scrollBy(10, 0);
                }
                return true;
            }
        });
    }
}
