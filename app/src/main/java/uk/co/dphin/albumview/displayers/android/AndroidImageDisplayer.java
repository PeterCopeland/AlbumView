package uk.co.dphin.albumview.displayers.android;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.logic.Controller;
import uk.co.dphin.albumview.logic.Dimension;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.ui.android.widgets.SlideFrameLayout;

import android.view.*;
/**
 * TODO: BitmapRegionDecoder may be useful to display the image full size:
 * http://stackoverflow.com/questions/6518215/display-huge-images-in-android
 */
public class AndroidImageDisplayer extends AndroidDisplayer implements ImageDisplayer
{
	/**
	 * Loaded image data for this slide.
	 * Maps from size constant to bitmap data
	 * TODO: Might need a view at each size too
	 */
	private HashMap<Dimension, Bitmap> images;
	
	/**
	 * Views for this slide (suitable for passing around Android UIs)
	 */
	private HashMap<Dimension, FrameLayout> views;

	/**
	 * Path to the image for this slide
	 */
	private String imagePath;
	
	private int imageWidth = -1;
	private int imageHeight = -1;
	private boolean imagePortrait;
	private boolean imagePanoramic;
	
	public AndroidImageDisplayer(ImageSlide s)
	{
		super(s);
		imagePath = s.getImagePath();
		images = new HashMap<Dimension, Bitmap>();
		views = new HashMap<Dimension, FrameLayout>();
	}
	
	/**
	 * Load the image at the specified size
	 */
	public void load(final int size)
	{
		Dimension dim = Controller.getController().getSize(size);
		
		// If we already have data at this size, assume it's valid
		if (images.containsKey(dim))
			return;
		
		loadMetaData();
		
		// Loading a large, multi-megapixel image can make a small device run out of memory.
		// We want to load several.
		// So we need to scale down the image to match the screen size as we load it.
		
		// Refuse to load if the dimensions aren't ready yet
		if (dim.width < 2 || dim.height < 2)
			return;
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		
		// If the image is portrait, and we're displaying full screen, rotate the image dimensions so it loads at screen resolution instead of height = width
		// Copy the object-level image dimensions and flip them if necessary to match screen & image orientation 
		int imageHeight = this.imageHeight;
		int imageWidth = this.imageWidth;
		if (size == Displayer.Size_Screen && ((imagePortrait && (dim.width > dim.height)) || !imagePortrait && dim.width < dim.height))
		{
			int tmp = imageWidth;
			imageWidth = imageHeight;
			imageHeight = tmp;
		}
		
		if (imageHeight > dim.height || imageWidth > dim.width)
		{
			// Calculate ratios of height and width to requested height and width
        	final int heightRatio = Math.round((float) imageHeight / (float) dim.height);
    	    final int widthRatio = Math.round((float) imageWidth / (float) dim.width);

        	// Choose the smallest ratio as inSampleSize value, this will guarantee
        	// a final image with both dimensions larger than or equal to the
       		// requested height and width.
        	options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		else
		{
			// Image smaller than we need, so load the image as normal
			options.inSampleSize = 1;
		}
		
		Bitmap img = BitmapFactory.decodeFile(imagePath, options);
		
		if (img == null)
		{
			Log.w("Load image", "Could not load image "+imagePath);
			return;
		}
		
		// Rotate the image to fill the screen (full size only). Must use un-rotated dimensions here.
		if (size >= Displayer.Size_Screen && this.imageHeight > this.imageWidth)
		{
			Matrix rotate = new Matrix();
			rotate.postRotate(90);
			img = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), rotate, true);
		}
				
		// Resize to fit dimensions (in case decoded file is larger than max texture size)
		if (dim.width > 0 && dim.height > 0 && (img.getWidth() > dim.width || img.getHeight() > dim.height))
		{
			// Calculate target dimensions so that the image JUST fits the available space
			double imgAR = (double)img.getWidth()/(double)img.getHeight();
			double outAR = (double)dim.width/(double)dim.height;
			int resizeW, resizeH;
			if (imgAR > outAR)
			{
				// Image is wider than display, fit width
				resizeW = dim.width;
				resizeH = (int)((double)resizeW/imgAR);
			}
			else
			{
				// Image is taller than display, fit height
				resizeH = dim.height;
				resizeW = (int)((double)resizeH * imgAR);
			}

			img = Bitmap.createScaledBitmap(img, resizeW, resizeH, false);
		}
		
		images.put(dim, img);
		
		// If the view for this size already exists, add the image
		synchronized(views)
		{
			if (getPlayContext() instanceof Activity && views.containsKey(dim))
			{
				Activity context = (Activity)getPlayContext();
				context.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						getView(size);
					}
				});
			}
		}
	}
	
	
	/**
	 * Loads image metadata. Only need to do this when we're displaying the image for the first time.
	 */
	private void loadMetaData()
	{
		if (this.imagePath != null && imageWidth < 0)
		{
			// Check the size of the image, which determines scale factors when we load
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(imagePath, options);
			
			imageHeight = options.outHeight;
			imageWidth = options.outWidth;
			imagePortrait = (options.outHeight > options.outWidth);
			
			// Consider an image panoramic if it's wider than about 16:9.
			if (imagePortrait)
				imagePanoramic = (((float)imageHeight/(float)imageWidth) > 1.8);
			else
				imagePanoramic = (((float)imageWidth/(float)imageHeight) > 1.8);
			
		}
	}
	
	public void deselected()
	{
		// Nothing
	}
		
	/**
	 * Destroy the reduced size image to free memory
	 * We keep the path, so can recreate the slide if necessary
	 */
	@Override
	public void doUnload(int size)
	{
		Dimension dim = Controller.getController().getSize(size);
		if (views.containsKey(dim))
		{
			ViewGroup v = views.get(dim);
			// Remove the imageView
			v.removeAllViews();
			
			// Delete the view
			views.remove(v);
		}
		if (images.containsKey(dim))
		{
			Log.i("Unload", imagePath+": unloaded size "+size+" at "+dim);
			Bitmap img = images.get(dim);
			images.remove(dim);
		}
		else
			Log.w("Unload", imagePath+": can't unload size "+size+" at "+dim);
	}
	
	/**
	 * Loads an image for this displayer.
	 * The image is loaded at reduced size to save memory, particularly on small devices
	 * TODO: If we allow zooming in, we'll need a full-size image.
	 * Perhaps load a small image in advance so the images swap smoothly, 
	 * then load the full size image as this image is selected or zoomed.
	 * Dispose of the full size image when no longer needed.
	 * TODO: Images should be loaded in a separate thread
	 * TODO: Rename this method to avoid confusion with private void loadImage
	 * @param imagePath Path to the image
	 */
	public void setImage(String imagePath)
	{
		this.imagePath = imagePath;
	}
			
	public View getView(int size)
	{
		FrameLayout frame;
		Dimension dim = Controller.getController().getSize(size);
		
		synchronized(views)
		{
			// Create or get an outer frame for this size
			if (views.containsKey(dim))
			{
				frame = views.get(dim);
			}
			else
			{
				frame = new SlideFrameLayout(getPlayContext(), getSlide());
				frame.setMinimumHeight(dim.height);
				frame.setMinimumWidth(dim.width);
				frame.setLayoutParams(new LinearLayout.LayoutParams(dim.width, dim.height));
				
				// TODO: Remove - testing
				ImageView testIV = new ImageView(getPlayContext());
				testIV.setImageResource(R.drawable.music);
				views.put(dim, frame);
			}
			
			// If we have image data, attach an image view to the frame
			if (images.containsKey(dim) && frame.getChildCount() == 0)
			{
				ImageView iv = new ImageView(getPlayContext());
				iv.setImageBitmap(images.get(dim));
				frame.addView(iv);
			}
		}
		
		return frame;
	}
	
	public boolean isPanoramic()
	{
		return this.imagePanoramic;
	}
	
	public Bitmap getImage(int size)
	{
		Dimension dim = Controller.getController().getSize(size);
		
		load(size);
		
		return images.get(dim);
	}
	
	public boolean isSizeLoaded(int size)
	{
		Dimension dim = Controller.getController().getSize(size);
		return images.containsKey(dim);
	}
	
	
}
