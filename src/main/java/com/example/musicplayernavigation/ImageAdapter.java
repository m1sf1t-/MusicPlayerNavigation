package com.example.musicplayernavigation;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
public class ImageAdapter extends BaseAdapter{
    private Context mContext;
    private String[] web;
    private ImageDBAdapter idb;
    private Bitmap[] bitmaps = null;
    
      public ImageAdapter(Context c, String[] web, Bitmap[] bitmaps) {
          mContext = c;
          this.web = web;
          this.idb = new ImageDBAdapter(mContext).open();
          this.bitmaps = bitmaps;
      }
    @Override
    public int getCount() {
      // TODO Auto-generated method stub
      return web.length;
    }
    @Override
    public Object getItem(int position) {
      // TODO Auto-generated method stub
      return null;
    }
    @Override
    public long getItemId(int position) {
      // TODO Auto-generated method stub
      return 0;
    }
    @SuppressLint("NewApi")
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View grid;
      LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      grid = new View(mContext);
      grid = inflater.inflate(R.layout.grid_layout, null);
      TextView textView = (TextView) grid.findViewById(R.id.grid_text);
      ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
      textView.setText(web[position]);
              
            
      if(bitmaps[position] != null){
       	imageView.setImageBitmap(bitmaps[position]);
      }else{
        imageView.setImageResource(R.drawable.default_background_2);
      }
      
	// textView.setBackgroundColor(Color.argb(175, 0, 153, 201));
      textView.setBackgroundColor(Color.argb(175, 0, 153, 204));
      
      return grid;
    }
    
    public void refillItems(String[] web, Bitmap[] bitmaps){
    	this.bitmaps = null;
    	this.web = null;
    	
    	this.bitmaps = bitmaps;
    	this.web = web;
    	
    	notifyDataSetChanged();
    	
    }
}
