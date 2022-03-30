package com.kmcaps.jbk.androidreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kmcaps.jbk.androidreader.R;

import org.w3c.dom.Text;

public class Adapter extends PagerAdapter {
    private int[]  images = {R.drawable.one,R.drawable.two,R.drawable.three,R.drawable.four,R.drawable.five,R.drawable.last};
    private LayoutInflater  inflater;
    private Context context;

    public Adapter(Context context) {
        this.context = context;
    }
    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public boolean isViewFromObject( View view, Object object) {
        return view == ((View) object);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position){
        inflater =(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v =inflater.inflate(R.layout.slider,container,false);
        ImageView imageView =(ImageView)v.findViewById(R.id.imageView);
        ImageView imageStep =(ImageView)v.findViewById(R.id.imageStep);
        TextView textTip =(TextView)v.findViewById(R.id.textTip);

        imageView.setImageResource(images[position]);


        switch (position){
            case 0: textTip.setText("어플실행시 기본 카메라 어플이 실행됩니다, 그후 원하시는 사진을 초점에 맞추어 촬영합니다.");
                imageStep.setImageResource(R.drawable.step1);
                break;
            case 1: textTip.setText("사진이 잘못 촬영시 다시시도 버튼을 누르시고 아니면 완료버튼을 누릅니다.");
                imageStep.setImageResource(R.drawable.step2);
                break;
            case 2: textTip.setText("원하시는 영역을 선택 하거나 회전 할수있습니다.");
                imageStep.setImageResource(R.drawable.step3);
                break;
            case 3: textTip.setText("선택된 영역의 글자들을 출력해서 보여줍니다.");
                imageStep.setImageResource(R.drawable.step4);
                break;
            case 4: textTip.setText("원하시는 글자를 선택해서 메뉴를 띄웁니다. 그다음 원하시는 영역을 선택하시고 번역버튼을 누릅니다.");
                imageStep.setImageResource(R.drawable.step5);
                break;
            case 5: textTip.setText("번역된 글자를 출력합니다.");
            imageStep.setImageResource(R.drawable.step6);
                break;
        }

        container.addView(v);
        return  v;

    }

    @Override
    public  void destroyItem(ViewGroup container, int position, Object object){
        container.invalidate();
    }
}
