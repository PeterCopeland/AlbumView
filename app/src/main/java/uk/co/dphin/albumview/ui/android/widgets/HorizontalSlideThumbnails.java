package uk.co.dphin.albumview.ui.android.widgets;

import android.app.Activity;
import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.storage.android.AlbumManager;
import uk.co.dphin.albumview.ui.android.*;

/**
 * A HorizontalScrollView that only loads the slides it needs to display
 */
public class HorizontalSlideThumbnails extends HorizontalScrollView implements View.OnDragListener
{
	private SlideListing context;
	
	private int thumbnailWidth;
	private int thumbnailHeight;
	private int numVisible;
	
	private Album album;
	private LinearLayout contents;
	
	//public HorizontalSlideThumbnails(Context context, AttributeSet attrs)
	//{
	//	super(context, attrs);
	//	this(context)
//
	//	LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	//	View view=layoutInflater.inflate(R.layout.,this);
//	}
 
	
	public HorizontalSlideThumbnails(Context c)
	{
		super(c);
		
		context = (SlideListing)c;
		
	}

	public void setThumbnailHeight(int thumbnailHeight)
	{
		this.thumbnailHeight = thumbnailHeight;
	}

	public int getThumbnailHeight()
	{
		return thumbnailHeight;
	}

	public void setThumbnailWidth(int thumbnailWidth)
	{
		this.thumbnailWidth = thumbnailWidth;
	}

	public int getThumbnailWidth()
	{
		return thumbnailWidth;
	}
	
	public void setContents(LinearLayout contents)
	{
		this.contents = contents;
	}
	
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		super.setLayoutParams(params);
		
		thumbnailWidth = 144;//getHeight();
		thumbnailHeight = 144;//getHeight();
		numVisible = getWidth() / thumbnailWidth;
	}
	
	public void setAlbum(Album a)
	{
		album = a;
		updateAlbumView();
	}
	
	public void updateAlbumView()
	{
		// Create image views for each slide
		int i=0;
		for (Slide s : album.getSlides())
		{
			// TODO: Use same as main view
			AndroidDisplayer disp = (AndroidDisplayer)s.getDisplayer();
			disp.setPlayContext(context);
			
			final View view = disp.getView(Displayer.Size_Thumb);
			view.setId(i++); // Get i, then increment for the next slide
			view.setOnClickListener(new OnClickListener() {
					public void onClick(View v)
					{
						// Add an image if we're missing one when the item is clicked
						Slide clickedSlide = album.getSlides().get(v.getId());
						AndroidDisplayer disp = (AndroidDisplayer)clickedSlide.getDisplayer();
						if (!disp.isSizeLoaded(Displayer.Size_Thumb)) {
							disp.load(Displayer.Size_Thumb);
						}
						context.selectSlide(v);
					}
				});
			view.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					// TODO: Is it better to represent the dragged slide as the slide itself, or its number?
					Slide draggedSlide = album.getSlides().get(v.getId());
					ClipData.Item item = new ClipData.Item(new Integer(v.getId()).toString());
					ClipData dragData = ClipData.newPlainText(item.getText(), item.getText());
					View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
					v.startDrag(dragData, shadow, draggedSlide, 0);

					return true;
				}
			});
			view.setOnDragListener(this);
			// Drag over the sort options to scroll left
			View sortOptions = ((Activity)getContext()).findViewById(R.id.sortOptions);
			sortOptions.setOnDragListener(new OnDragListener() {
				@Override
				public boolean onDrag(View v, DragEvent event) {
					switch(event.getAction()) {
						case DragEvent.ACTION_DRAG_LOCATION:
							HorizontalSlideThumbnails.this.scrollBy((10*-1), 0);
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
							HorizontalSlideThumbnails.this.scrollBy(10, 0);
					}
					return true;
				}
			});
			if (view.getParent() != null)
			{
				((ViewGroup)view.getParent()).removeView(view);
			}
			
			if (view.getParent() == null)
			{
				contents.addView(view);
				invalidate();
			}
			
			//ImageView testIV = new ImageView(context);
			//testIV.setImageBitmap(((AndroidImageDisplayer)disp).getImage(Displayer.Size_Thumb));
			//testIV.setAlpha(0.5f);
			//contents.addView(testIV);
		}
		
		updateDisplay(getScrollX());
	}
	
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt)
	{
		updateDisplay(l);
	}
	
	private void updateDisplay(int left)
	{
		// What are the first and last thumbnails that are now visible
		
		// Also load one either side to make sure anything sticking out is rendered
		int firstVisible = Math.max(0, (left / thumbnailWidth)-1);
		int lastVisible = Math.min(contents.getChildCount()-1, firstVisible + numVisible + 2);

		ListIterator<Slide> iter = album.getSlides().listIterator(firstVisible);
		int numDone = 0;
		while (iter.hasNext() && numDone < numVisible + 2)
		{
			Slide s = iter.next();
			Displayer d = s.getDisplayer();
			if (!d.isSizeLoaded(Displayer.Size_Thumb))
				d.load(Displayer.Size_Thumb);
			numDone++;
		}
	}

	/**
	 * Called when a drag event is dispatched to a view. This allows listeners
	 * to get a chance to override base View behavior.
	 *
	 * @param v     The View that received the drag event.
	 * @param event The {@link DragEvent} object for the drag event.
	 * @return {@code true} if the drag event was handled successfully, or {@code false}
	 * if the drag event was not handled. Note that {@code false} will trigger the View
	 * to call its {@link #onDragEvent(DragEvent) onDragEvent()} handler.
	 *
	 * @todo All this should be on the edit page only, not AlbumPlay. This code is shared...
	 */
	@Override
	public boolean onDrag(View v, DragEvent event) {
		final int action = event.getAction();

		switch (action)
		{
			case DragEvent.ACTION_DRAG_STARTED:
				break;
			case DragEvent.ACTION_DRAG_ENTERED:
				break;
			case DragEvent.ACTION_DRAG_LOCATION:
				Log.i("DragEventLocation", v.getClass().getSimpleName()+" "+v.getId()+" ("+event.getX()+","+event.getY()+")");
				// If we're near the edge of the filmstrip, scroll in that direction
//				if ()
				break;
			case DragEvent.ACTION_DROP:
				if (v instanceof SlideFrameLayout) {
					SlideFrameLayout slideFrame = (SlideFrameLayout)v;
					Slide draggedSlide = (Slide)event.getLocalState();
					Slide droppedOnSlide = slideFrame.getSlide();
					// If the dragged slide was dropped on the left half of the target slide, move it before. Otherwise put it after.
					boolean droppedBefore = (event.getX() <= slideFrame.getWidth()/2);
					Album album = ((SlideListing)getContext()).getAlbum();
					if (droppedBefore) {
						album.moveSlideBefore(draggedSlide, droppedOnSlide);
					} else {
						album.moveSlideAfter(draggedSlide, droppedOnSlide);
					}

					((SlideListing)getContext()).updateThumbnails();
					if (getContext() instanceof AlbumEdit) {
						((AlbumEdit)getContext()).saveAlbum();
					}

					return true;
				}
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				break;
		}
		return true;
	}

	private class SlideDragListener implements View.OnDragListener
	{
		/**
		 * Called when a drag event is dispatched to a view. This allows listeners
		 * to get a chance to override base View behavior.
		 *
		 * @param v     The View that received the drag event.
		 * @param event The {@link DragEvent} object for the drag event.
		 * @return {@code true} if the drag event was handled successfully, or {@code false}
		 * if the drag event was not handled. Note that {@code false} will trigger the View
		 * to call its {@link #onDragEvent(DragEvent) onDragEvent()} handler.
		 */
		@Override
		public boolean onDrag(View v, DragEvent event) {
			final int action = event.getAction();

			switch (action)
			{
				case DragEvent.ACTION_DRAG_STARTED:
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					break;
				case DragEvent.ACTION_DRAG_LOCATION:
					break;
				case DragEvent.ACTION_DROP:
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					break;
			}
			return true;
		}
	}
}
