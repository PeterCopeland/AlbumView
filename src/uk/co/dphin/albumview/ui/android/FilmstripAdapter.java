package uk.co.dphin.albumview.ui.android;

import android.content.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;

import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.*;
import uk.co.dphin.albumview.models.*;

public class FilmstripAdapter extends ArrayAdapter<Slide>
{
	public FilmstripAdapter(Context context, int resource, List<Slide> slides)
	{
		super(context, resource, slides);
	}
	
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Toast.makeText(getContext(), "GetView: "+position, Toast.LENGTH_SHORT);
		Log.i("Filmstrip", "GetView for item "+position);
		ImageView img;
		
		if (convertView == null)
			img = new ImageView(this.getContext());
		else
			img = (ImageView)convertView;
			
		ImageSlide slide = (ImageSlide)this.getItem(position);
		AndroidImageDisplayer disp = (AndroidImageDisplayer)slide.getDisplayer();
		disp.load(Displayer.Size_Thumb);
		img.setImageBitmap(disp.getImage(Displayer.Size_Thumb));
		
		return img;
	}
}
