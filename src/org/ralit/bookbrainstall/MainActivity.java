package org.ralit.bookbrainstall;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import com.artifex.mupdfdemo.MuPDFCore;

import jp.recognize.HttpSceneryLineLayoutAnalysisRequest;
import jp.recognize.SceneryLineLayoutAnalyzer;
import jp.recognize.client.HttpSceneryLineLayoutAnalyzer;
import jp.recognize.common.ImageContentType;
import jp.recognize.common.RecognitionResult.LineLayout;
import jp.recognize.common.Shape.Rectangle;
import jp.recognize.common.client.HttpSceneryRecognitionRequest.InputStreamImageContent;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Config;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class MainActivity extends Activity implements AnimatorListener{
	
	private LinearLayout linearlayout;
	private FrameLayout framelayout2;
	private FrameLayout framelayout;
	private FrameLayout rootframe;
	private ImageView image;
	private ImageView image2;
	private ImageView select;
	private ImageView overview;
	private ImageView nowloadingview;
	private BitmapFactory.Options options;
	private float dH;
	private float dW;
	private float textZoom;
	private boolean focusChanged = false; 
	private boolean first = true;
	private boolean back = false;
	private float cH;
	private float cW;
	private Bitmap bmp;
	private Bitmap mutableBitmap;
	private Bitmap page;
	ArrayList<ArrayList<Integer>> pos = new ArrayList<ArrayList<Integer>>();
	private Paint frame;
	private Paint number;
	ObjectAnimator fadein;
	ObjectAnimator fadeout;
	ObjectAnimator move;
	private String tag = "ralit";
	private GestureDetector gesture;
	AnimatorSet set;
	private MuPDFCore pdf;
	
	private String RECOGNITION_URL = "https://recognize.jp/v1/scenery/api/line-region";
	private String API_KEY = "kU10DrMKI3xRnv4RVcxqbR1slGwrfTCsSKoc9A378s";
	private String ANALYSIS = "standard";
	private int index = 0;
	private byte[] jpegData;
	private LineLayout[] job;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(tag, "onCreate()");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		initRootView();
//		rootframe.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		nowloadingview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		Log.i(tag, "onWindowFocusChanged()");
		super.onWindowFocusChanged(hasFocus);
		if (focusChanged) { return; }
		focusChanged = true;
		initChildrenView();
//		linearlayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		framelayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		framelayout2.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		image2.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		overview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		fadeinNowloading();
	}

	@Override
	public void onAnimationEnd(Animator animation) {
		Log.i(tag, "onAnimationEnd()");
		if (first) {
			first = false;
			recognize();
			setPosition();
			Collections.sort(pos, new PositionComparator());
			deleteDuplicate();
			paintPosition();
			savePaintedImage();
			setimage();
			setimage2();
			fadeoutNowloading();
			gesture = new GestureDetector(this, gestureListener);
			animation();
			Log.i(tag, "image" + image.isHardwareAccelerated());
			Log.i(tag, "framelayout" + framelayout.isHardwareAccelerated());
			Log.i(tag, "rootframe" + rootframe.isHardwareAccelerated());
			Log.i(tag, "image" + image.getLayerType());
			Log.i(tag, "framelayout" + framelayout.getLayerType());
			Log.i(tag, "rootframe" + rootframe.getLayerType());
			
		} else {
			if (!back) { ++index; }
			back = false;
			Log.i(tag, "index: " + index);
			if (index < pos.size()) {
				setimage();
				animation2();
				animation();	
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return gesture.onTouchEvent(ev);
	}
	
	private final SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
		@Override
		public boolean onFling(MotionEvent ev1, MotionEvent ev2, float vx, float vy) {
				if (Math.abs(ev1.getY() - ev2.getY()) > 250) { return false; }
				if (ev2.getX() - ev1.getX() > 120 && Math.abs(vx) > 200) {
					back = true;
					if (set.getChildAnimations().get(0).isRunning()) { 
						if (index > 0) { --index; }
					}
					set.cancel();
				}
				if (ev1.getX() - ev2.getX() > 120 && Math.abs(vx) > 200) {
					set.cancel();
				}
			return false;
		}
	};
	
	private void deleteDuplicate() {
		Log.i(tag, "deleteDuplicate()");
		for (int i = 1; i < pos.size();) {
			ArrayList<Integer> i0 = pos.get(i);
			ArrayList<Integer> i1 = pos.get(i - 1);
			if (i0.get(1) < i1.get(3)) {  
				i1.set(0, i0.get(0) < i1.get(0) ? i0.get(0) : i1.get(0));
				i1.set(2, i0.get(2) > i1.get(2) ? i0.get(2) : i1.get(2));
				pos.remove(i);
			} else {
				++i;
			}
		}
	}
	
	public void initRootView() {
		Log.i(tag, "initRootView()");
		rootframe = new FrameLayout(this);
		nowloadingview = new ImageView(this);
		nowloadingview.setAlpha(0f);
		rootframe.addView(nowloadingview);
		setContentView(rootframe);
	}	
	

	private void initChildrenView() {
		Log.i(tag, "initChildrenView()");
		Log.i(tag, "rootframe" + rootframe.getHeight() + rootframe.getWidth());
		linearlayout = new LinearLayout(this);
		linearlayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearlayout.setOrientation(1); // vertical
		rootframe.addView(linearlayout);
		
		framelayout = new FrameLayout(this);
		framelayout.setLayoutParams(new LayoutParams(rootframe.getWidth(), rootframe.getHeight() / 2));
		framelayout2 = new FrameLayout(this);
		framelayout2.setLayoutParams(new LayoutParams(rootframe.getWidth(), rootframe.getHeight() / 2));
		linearlayout.addView(framelayout);
		linearlayout.addView(framelayout2);
		
		image = new ImageView(this);
		image2 = new ImageView(this);
		image2.setAlpha(0f);
		overview = new ImageView(this);
		framelayout.addView(image);
		framelayout.addView(image2);
		framelayout2.addView(overview);
	}
	
	public void fadeinNowloading() {
		Log.i(tag, "fadeinNowloading()");
		nowloadingview.setImageResource(com.artifex.mupdfdemo.R.drawable.recognizing);
		ObjectAnimator anim = ObjectAnimator.ofFloat(nowloadingview, "alpha", 1f);
		anim.setDuration(1000);
		anim.addListener(this);
		anim.start();
	}
	
	public void fadeoutNowloading() {
		Log.i(tag, "fadeoutNowloading()");
		ObjectAnimator anim = ObjectAnimator.ofFloat(nowloadingview, "alpha", 0f);
		anim.setDuration(1000);
		anim.start();
	}
	
	public void animation() {
		Log.i(tag, "animation()");
		set = new AnimatorSet();
		if(index % 2 == 0) {
			fadein = ObjectAnimator.ofFloat(image, "alpha", 0f, 1f);
			fadeout = ObjectAnimator.ofFloat(image2, "alpha", 1f, 0f);
		} else if(index % 2 == 1) {
			fadein = ObjectAnimator.ofFloat(image2, "alpha", 0f, 1f);
			fadeout = ObjectAnimator.ofFloat(image, "alpha", 1f, 0f);
		}
		fadein.setDuration(1000);
		fadeout.setDuration(1000);
		move = ObjectAnimator.ofFloat(select, "x", dW * textZoom / (float)2, -dW * textZoom / (float)2);
		move.setDuration(15000);
		move.setInterpolator(new LinearInterpolator());
		set.play(fadein).with(fadeout);
		set.play(fadein).before(move);
		set.addListener(this);
		set.start();
	}
	
	private void animation2() {
		float h = mutableBitmap.getHeight();
		float w = mutableBitmap.getWidth();
		float linemid = (pos.get(index).get(3) + pos.get(index).get(1)) / 2;
		float distance = h / 2 - linemid;
		float i = distance * (overview.getWidth() / w);
		ObjectAnimator anim = ObjectAnimator.ofFloat(overview, "y", i);
		anim.setDuration(1000);
		anim.start();
	}
	
	private void prepare_image() {
		Log.i(tag, "prepare_image()");
		dH = (float) framelayout.getHeight();
		dW = (float) framelayout.getWidth();
		cW = (pos.get(index).get(2) - pos.get(index).get(0));
		cH = (pos.get(index).get(3) - pos.get(index).get(1));
		textZoom = dH / (cH * (dW/cW));
		Log.i("dH", Float.toString(dH));
		Log.i("dW", Float.toString(dW));
		Log.i("cW", Float.toString(cW));
		Log.i("cH", Float.toString(cH));
		Log.i("textZoom", Float.toString(textZoom));
	}
	
	public void setimage() {
		Log.i(tag, "setimage()");
		if (index % 2 == 0) { select = image; } else { select = image2; }
		int w = pos.get(index).get(2) - pos.get(index).get(0);
		int h = pos.get(index).get(3) - pos.get(index).get(1);
//		select.setImageBitmap(Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, pos.get(index).get(0), pos.get(index).get(1), w, h), 2048, (int)(2048 * ((float)h/(float)w)), false));
		select.setImageBitmap(Bitmap.createBitmap(bmp, pos.get(index).get(0), pos.get(index).get(1), w, h));
		prepare_image();
		select.setScaleX(textZoom);
		select.setScaleY(textZoom);
		select.setX(dW * textZoom / (float)2);
		select.setY(0);
	}
	
	private void setimage2() {
		Log.i(tag, "setimage2()");
		float w = (float) mutableBitmap.getWidth();
		float h = (float) mutableBitmap.getHeight();
		float ratio = dH / h;
		float small_w = w * ratio;
		float scale_ratio = dW / small_w;
		page = Bitmap.createScaledBitmap(mutableBitmap, (int)dW, (int)(dW * (h/w)), false);
		overview.setImageBitmap(page);

		overview.setScaleX(scale_ratio);
		overview.setScaleY(scale_ratio);
		float linemid = (pos.get(index).get(3) + pos.get(index).get(1)) / 2;
		float distance = h / 2 - linemid;
		float i = distance * (overview.getWidth() / w);
		overview.setY(i);
		Log.i(tag, "i: " + i);
	}
	
	private void recognize() {
		Log.i(tag, "recognize()");
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				docomo();
			}
		});
		try {
			thread.start();
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
		
	private void docomo () {
		Log.i(tag, "docomo()");
		
		File file = new File(Environment.getExternalStorageDirectory().getPath() + "/imagemove/");
		String attachName = file.getAbsolutePath() + "/" + "file.pdf";
		try {
			pdf = new MuPDFCore(this, attachName);
			int page_max = pdf.countPages();
			PointF size = new PointF();
			size = pdf.getPageSize(0);
			bmp = Bitmap.createBitmap((int)size.x, (int)size.y, android.graphics.Bitmap.Config.ARGB_8888);
			pdf.drawPage(bmp, 0, (int)size.x, (int)size.y, 0, 0, (int)size.x, (int)size.y);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
//		options = new BitmapFactory.Options();
//		options.inScaled = false;
//		bmp = BitmapFactory.decodeResource(getResources(), com.artifex.mupdfdemo.R.drawable.organic, options);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 90, bos);
		jpegData = bos.toByteArray();
		
		try {
			SceneryLineLayoutAnalyzer analyzer;
			analyzer = new HttpSceneryLineLayoutAnalyzer(new URL(RECOGNITION_URL));
			job = analyzer.analyze(new HttpSceneryLineLayoutAnalysisRequest(
					API_KEY, 
					ANALYSIS, 
					new InputStreamImageContent(ImageContentType.IMAGE_JPEG, new ByteArrayInputStream(jpegData)),
					null /* new HttpSceneryLineLayoutAnalysisHint(aImageTrimRectangle, aImageRotationDegree, aLetterColor) */
					));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	private void setPosition() {
		Log.i(tag, "setPosition()");
		for (LineLayout line : job) {
			Rectangle bounds = line.getShape().getBounds();
			ArrayList<Integer> internal = new ArrayList<Integer>();
			float marginRatio = 0.2f;
			float margin = (bounds.getBottom() - bounds.getTop()) * marginRatio;
			internal.add(bounds.getLeft() - (int)margin);
			internal.add(bounds.getTop() - (int)margin);
			internal.add(bounds.getRight() + (int)margin);
			internal.add(bounds.getBottom() + (int)margin);
			pos.add(internal);	
		}
	}
	
	private void paintPosition() {
		Log.i(tag, "paintPosition()");
		frame = new Paint();
		frame.setStyle(Style.STROKE);
		frame.setColor(Color.RED);
		frame.setStrokeWidth(4);
		number = new Paint();
		number.setStyle(Style.FILL_AND_STROKE);
		number.setColor(Color.RED);
		number.setStrokeWidth(1);
		number.setTextSize(20);
		mutableBitmap = bmp.copy(bmp.getConfig(), true);
		Canvas canvas = new Canvas(mutableBitmap);
		for (int i = 0; i < pos.size(); ++i) {
			Rect rect = new Rect(pos.get(i).get(0), pos.get(i).get(1), pos.get(i).get(2), pos.get(i).get(3));
			canvas.drawRect(rect, frame);
			canvas.drawText(Integer.toString(i), pos.get(i).get(0), pos.get(i).get(1), number);
		}
	}
	
	private void printPosition() {
		Log.i(tag, "printPosition()");
		for (int i = 0; i < pos.size(); ++i) {
			Log.i("pos", i + ": " + pos.get(i));
		}
	}
	
	private void savePaintedImage() {
		Log.i(tag, "savePaintedImage()");
		// ファイルの保存
		Log.i(tag, Environment.getExternalStorageDirectory().getPath());
		File file = new File(Environment.getExternalStorageDirectory().getPath() + "/imagemove/");
		try {
			if (!file.exists()) { file.mkdir(); }
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		String attachName = file.getAbsolutePath() + "/" + "imagemove.jpg";
		try {
			FileOutputStream out = new FileOutputStream(attachName);
			mutableBitmap.compress(CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
//			mutableBitmap.recycle();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onAnimationCancel(Animator animation) {
		Log.i(tag, "onAnimationCancel()");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationRepeat(Animator animation) {
		Log.i(tag, "onAnimationRepeat()");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationStart(Animator animation) {
		Log.i(tag, "onAnimationStart()");
		// TODO Auto-generated method stub
		
	}
}

class PositionComparator implements java.util.Comparator<ArrayList<Integer>> {
	@Override
	public int compare(ArrayList<Integer> lhs, ArrayList<Integer> rhs) {
		return lhs.get(1) - rhs.get(1);
	}
}
