package uk.co.dphin.albumview.ui.android.widgets;

import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import uk.co.dphin.albumview.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.ui.android.*;

/**
 * A HorizontalScrollView that only loads the slides it needs to display
 */
public class HorizontalSlideThumbnails extends HorizontalScrollView
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
			disp.load(Displayer.Size_Thumb);// TODO: From loader thread
			
			final View view = disp.getView(Displayer.Size_Thumb);
			view.setId(i++); // Get i, then increment for the next slide
			view.setOnClickListener(new OnClickListener() {
					public void onClick(View v)
					{
						// Add an image if we're missing one when the item is clicked
						Slide clickedSlide = album.getSlides().get(v.getId());
						AndroidDisplayer disp = (AndroidDisplayer)clickedSlide.getDisplayer();
						if (!disp.isSizeLoaded(Displayer.Size_Thumb))
							disp.load(Displayer.Size_Thumb);
						
						context.selectSlide(v);
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
		// TODO: May need to unload formerly visible thumbnails to save RAM
		updateDisplay(l);
	}
	
	private void updateDisplay(int left)
	{
		// TODO: Not working - width comes up as 0 so only loads 2 slides at best
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
		//Log.i("HorizontalSlideThumbnails", "Update display at "+left+", updated "+numDone+" slides starting at "+firstVisible+" - last visible is "+lastVisible);
	}
}
