package com.kmcaps.jbk.androidreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import android.graphics.Canvas;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import android.graphics.Paint;

import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Parent {

    private static final int MULTIPLE_PERMISSIONS = 101; //권한 동의 여부 문의 후 CallBack 함수에 쓰일 변수

    private static final int PICK_FROM_CAMERA = 1; // 카메라 사진 촬영
    private static final int PICK_FROM_ALBUM = 2; // 카메라 앨범
    private static final int CROP_FROM_CAMERA = 3; // 사진 자르기

    Uri photoUri;

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};

    private ImageButton capturBtn;
    private ImageView capImg;
    private Bitmap img;
    private EditText ocr_Result;
    private RadioGroup rgp;
    ImageButton btnHelp;



    // OCR
    static TessBaseAPI sTess;
    private String lang = "eng";
    private String datapath = "";
    private boolean mStartFlag = false;

    // Translate
    private String texts = "";
    private EditText trans_Result;
    private String target = "ko";
    private String source = "en";
    String str;

    private ProgressBar pgb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateHide();
        softKeyHide();

        btnHelp = (ImageButton)findViewById(R.id.btnHelp);
        capturBtn = (ImageButton) findViewById(R.id.capturBtn);
        capImg = (ImageView) findViewById(R.id.imgView);
        ocr_Result = (EditText) findViewById(R.id.text_ocrresult);
        rgp = (RadioGroup) findViewById(R.id.radioGrp);
        pgb = (ProgressBar)findViewById(R.id.progressBar);
        trans_Result = (EditText) findViewById(R.id.text_transresult);

        checkPermission();

        btnHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,Pager.class);
                startActivity(intent);
            }
        });

        capturBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        ocr_Result.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.trans) {

                    pgb.setVisibility(View.VISIBLE);
                    copyText();
                    texts = clipboardManager.getText().toString();
                    trans_Result.setVisibility(View.VISIBLE);


                    /////////* 파파고 클래스 생성 및 번역 처리 */////////
                    PapagoTrans pThread = new PapagoTrans(texts,source,target);
                    pThread.start();

                    try{
                        pThread.join();
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    str = pThread.getResultText();

                    trans_Result.setText(str);
                    pgb.setVisibility(View.INVISIBLE);
                    ////////////////////////////////////////////////////

                    actionMode.finish();

                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });

        rgp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.korBtn:
                        lang = "kor";
                        Toast.makeText(MainActivity.this, "한글 번역 : " + lang, Toast.LENGTH_SHORT).show();

                        if (checkFile(new File(datapath + "/tessdata"))) {
                            sTess.init(datapath, lang);
                        }
                        source = "ko";
                        target = "en";

                        break;
                    case R.id.engBtn:
                        lang = "eng";
                        Toast.makeText(MainActivity.this, "영어 번역 : " + lang, Toast.LENGTH_SHORT).show();

                        if (checkFile(new File(datapath + "/tessdata"))) {
                            sTess.init(datapath, lang);
                        }
                        source = "en";
                        target = "ko";

                        break;
                    default:
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sTess = new TessBaseAPI();

        datapath = getFilesDir() + "/tesseract";

        if (checkFile(new File(datapath + "/tessdata"))) {
            sTess.init(datapath, lang);
        }
    }




    private void takePhoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try{
            photoFile = createImageFile();
        }catch(IOException e){
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요", Toast.LENGTH_SHORT).show();
        }

        if(photoFile != null){
            photoUri = FileProvider.getUriForFile(MainActivity.this,"com.kmcaps.jbk.provider",photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
            startActivityForResult(intent,PICK_FROM_CAMERA);
        }
    }

    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "IP" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Reader/");
        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    private void goToAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent,PICK_FROM_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK){
            return;
        }
        /*if(data == null)
            return;*/

        if(requestCode == PICK_FROM_ALBUM){
            photoUri = data.getData();
            cropImage();
        }else if(requestCode == PICK_FROM_CAMERA) {
            cropImage();
            MediaScannerConnection.scanFile(MainActivity.this,
                    new String[]{photoUri.getPath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {

                        }
                    });
        }else if(requestCode == CROP_FROM_CAMERA){
            try{
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),photoUri);
                Bitmap thumbImage = ThumbnailUtils.extractThumbnail(bitmap, bitmap.getWidth(), bitmap.getHeight());
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                thumbImage.compress(Bitmap.CompressFormat.JPEG, 100, bs); //이미지가 클 경우 OutOfMemoryException 발생이 예상되어 압축

                //여기서는 ImageView에 setImageBitmap을 활용하여 해당 이미지에 그림을 띄우시면 됩니다.
                capImg.setImageBitmap(thumbImage);
                img = androidGrayScale(thumbImage);
                pgb.setVisibility(View.VISIBLE);
                new AsyncTess().execute(img);
            }catch(IOException e){
                Log.e("ERROR",e.getMessage().toString());
            }catch(NullPointerException e){

            }
        }
    }

    public void cropImage(){
        this.grantUriPermission("com.android.camera",photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | getIntent().FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri,"image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,0);
        grantUriPermission(list.get(0).activityInfo.packageName,photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        int size = list.size();
        if(size == 0){
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }else{
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra("crop", "true");
            File croppedFileName = null;

            try{
                croppedFileName = createImageFile();
            }catch (IOException e){
                e.printStackTrace();
            }

            File folder = new File(Environment.getExternalStorageDirectory() + "/Reader/");
            File tempFile = new File(folder.toString(), croppedFileName.getName());

            photoUri = FileProvider.getUriForFile(MainActivity.this,"com.kmcaps.jbk.provider",tempFile);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
            intent.putExtra("outputFormat",Bitmap.CompressFormat.JPEG.toString());

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            grantUriPermission(res.activityInfo.packageName,photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            startActivityForResult(i,CROP_FROM_CAMERA);
        }

    }

    private void copyText() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        CharSequence selectedTxt = ocr_Result.getText().subSequence(ocr_Result.getSelectionStart(), ocr_Result.getSelectionEnd());
        ClipData clipData = ClipData.newPlainText("selected text", selectedTxt);
        clipboardManager.setPrimaryClip(clipData);
    }

    boolean checkFile(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/" + lang + ".traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
        return true;
    }

    void copyFiles() {
        AssetManager assetMgr = this.getAssets();

        InputStream is = null;
        OutputStream os = null;

        try {
            is = assetMgr.open("tessdata/" + lang + ".traineddata");
            String destFile = datapath + "/tessdata/" + lang + ".traineddata";

            os = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            is.close();
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "파일을 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "파일을 읽지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> {

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            //Tesseract OCR 수행
            sTess.setImage(img);
            return sTess.getUTF8Text();
        }

        @Override
        protected void onPostExecute(String s) {
            ocr_Result.setVisibility(View.VISIBLE);
            capImg.setImageBitmap(null);
            mStartFlag = true;
            ocr_Result.setText(s);
            pgb.setVisibility(View.INVISIBLE);
        }
    }

    private Bitmap androidGrayScale(final Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }


    private boolean checkPermission() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) { //사용자가 해당 권한을 가지고 있지 않을 경우 리스트에 해당 권한명 추가
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) { //권한이 추가되었으면 해당 리스트가 empty가 아니므로 request 즉 권한을 요청합니다.
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToast();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToast();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showNoPermissionToast();

                            }
                        }
                    }
                } else {
                    showNoPermissionToast();
                }
                return;
            }
        }
    }

    private void showNoPermissionToast() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
