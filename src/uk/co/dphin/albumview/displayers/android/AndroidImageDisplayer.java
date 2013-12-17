package uk.co.dphin.albumview.displayers.android;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import uk.co.dphin.albumview.displayers.*;
import uk.co.dphin.albumview.models.*;

public class AndroidImageDisplayer extends AndroidDisplayer implements ImageDisplayer
{

	/**
	 * Path to the image for this slide
	 */
	private String imagePath;
	
	/**
	 * Reduced (screen size) copy of the image
	 */
	private Bitmap image;
	
	/**
	 * True if the image loaded has been shrunk, false if it is full size
	 */
	private boolean imageIsReduced;
	
	/**
	 * Full size copy of the image, only loaded while image is on the screen.
	 * Used to allow zooming.
	 */
	private Bitmap fullImage;
	
	/**
	 * View to be given to a context displaying this displayer
	 */
	private ImageView view;
	
	public AndroidImageDisplayer(ImageSlide s)
	{
		super(s);
		imagePath = s.getImagePath();
	}
	
	/**
	 * Prepare the slide for display by loading a reduced copy of the image
	 * The image should be around the size of the screen to save memory
	 * A full size image could be loaded when the slide is activated
	 * to allow zooming
	 */
	public void prepare()
	{
		setState(Displayer.Preparing);
		loadImage();
		setState(Displayer.Prepared);
	}
	
	/**
	 * Just before displying the slide, if we're currently using
	 * a reduced resolution image, load the full size version
	 * FIXME: Implement a maximum resolution, or detect memory limits
	 */
	public void selected()
	{
		setState(Displayer.Loading);
		if (imageIsReduced)
		{
			this.fullImage = BitmapFactory.decodeFile(imagePath);
		}
		else
		{
			this.fullImage = this.image;
		}
		setState(Displayer.Loaded);
	}
	
	public void active()
	{
		// Nothing to do
	}
	
	public void deselected()
	{
		// Nothing
	}
	
	/**
	 * Destroy the full size image (if any) to free memory
	 */
	public void deactivated()
	{
		setState(Displayer.Prepared);
		// Only recycle the image now if the full size version is separate from the reduced version
		if (imageIsReduced && this.fullImage != null && !this.fullImage.isRecycled())
		{
			this.fullImage.recycle();
		}
		this.fullImage = null;
	}
	
	/**
	 * Destroy the reduced size image to free memory
	 * We keep the path, so can recreate the slide if necessary
	 */
	public void unload()
	{
		setState(Displayer.Unloaded);
		if (this.image != null && !this.image.isRecycled())
			this.image.recycle();
		this.image = null;
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
	public void loadImage(String imagePath)
	{
		this.imagePath = imagePath;
	}
			
	public View getView(Context context)
	{
		if (view == null)
		{
			view = new ImageView(context);
			view.setImageBitmap(getImage());
		}
		return view;
	}
	
	public Bitmap getImage()
	{
		//if (image == null || image.isRecycled())
			loadImage();
		
		return image;
	}
	
	private void loadImage()
	{
		// Loading a large, multi-megapixel image can make a small device run out of memory.
		// We want to load several.
		// So we need to scale down the image to match the screen size as we load it.

		// Get the original image dimensions first
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imagePath, options);
		int imageHeight = options.outHeight;
		int imageWidth = options.outWidth;
		String imageType = options.outMimeType;

		// Now find the best scale factor to display the image full screen
		options.inSampleSize = 1;

		// TODO: Could set exact sampleSize if we only ever use the image at screen size
    	if (hasDimensions() && (imageHeight > getHeight() || imageWidth > getWidth())) {

        	// Calculate ratios of height and width to requested height and width
        	final int heightRatio = Math.round((float) imageHeight / (float) getHeight());
    	    final int widthRatio = Math.round((float) imageWidth / (float) getWidth());

        	// Choose the smallest ratio as inSampleSize value, this will guarantee
        	// a final image with both dimensions larger than or equal to the
       		// requested height and width.
        	options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
			imageIsReduced = true;
    	}
		else
		{
			imageIsReduced = false;
		}

		// Load the image at the correct sample size
		options.inJustDecodeBounds = false;

		this.image = BitmapFactory.decodeFile(imagePath, options);
	}
	
	
}
