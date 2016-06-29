package br.com.meerkat.avapreview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by gfuhr on 6/27/16.
 */
public class ScalableImageView extends View {
    private final Drawable logo;

    public ScalableImageView(Context context) {
        super(context);
        logo = context.getResources().getDrawable(R.drawable.meerkat_logo_white);
        setBackgroundDrawable(logo);
    }

    public ScalableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        logo = context.getResources().getDrawable(R.drawable.meerkat_logo_white);
        setBackgroundDrawable(logo);
    }

    public ScalableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        logo = context.getResources().getDrawable(R.drawable.meerkat_logo_white);
        setBackgroundDrawable(logo);
    }

    @Override protected void onMeasure(int widthMeasureSpec,
                                       int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = width * logo.getIntrinsicHeight() / logo.getIntrinsicWidth();
        setMeasuredDimension(width, height);
    }
}
