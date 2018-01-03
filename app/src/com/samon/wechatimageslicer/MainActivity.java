package com.samon.wechatimageslicer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samon.wechatimageslicer.slicer.BitmapSlicer;
import com.samon.wechatimageslicer.slicer.FourPicBitmapSlicer;
import com.samon.wechatimageslicer.slicer.NinePicBitmapSlicer;
import com.samon.wechatimageslicer.slicer.SixPicBitmapSlicer;
import com.samon.wechatimageslicer.slicer.ThreePicBitmapSlicer;
import com.samon.wechatimageslicer.slicer.TowPicBitmapSlicer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PICK = 0;
    private static final int REQUEST_CODE_CUT = 1;
    private static final int REQUEST_PERMISSION = 2;
    private static final int MENU_2_PIC = 0;
    private static final int MENU_3_PIC = 1;
    private static final int MENU_4_PIC = 2;
    private static final int MENU_6_PIC = 3;
    private static final int MENU_9_PIC = 4;

    private static final String baseDir = Environment.getExternalStorageDirectory() + "/tencent/MicroMsg/WeiXin/Slices";
    private static final File tempFile = new File(baseDir, "crop_temp");

    private BitmapSlicer.BitmapSliceListener bitmapSliceListener = new BitmapSlicer.BitmapSliceListener() {
        @Override
        public void onSliceSuccess(Bitmap srcBitmap, List<Bitmap> desBitmaps) {
            srcBitmap.recycle();
            bitmapSlicer.setSrcBitmap(null);
            for (ImageView imageView : ninePicImageViews) {
                imageView.setImageBitmap(null);
                imageView.setVisibility(View.GONE);
            }
            if (lastDesBitmaps != null) {
                for (Bitmap lastDesBitmap : lastDesBitmaps) {
                    lastDesBitmap.recycle();
                }
            }
            lastDesBitmaps = null;
            for (int i = 0; i < currentImageViewList.size(); i++) {
                currentImageViewList.get(i).setImageBitmap(desBitmaps.get(i));
                currentImageViewList.get(i).setVisibility(View.VISIBLE);
            }
            lastDesBitmaps = desBitmaps;
            progressView.setVisibility(View.GONE);
            resultView.setVisibility(View.GONE);
        }

        @Override
        public void onSliceFailed() {
            Toast.makeText(MainActivity.this, "切片失败", Toast.LENGTH_SHORT).show();
            progressView.setVisibility(View.GONE);
            resultView.setVisibility(View.GONE);
        }
    };

    private BitmapSlicer ninePicBitmapSlicer = new NinePicBitmapSlicer();
    private BitmapSlicer towPicBitmapSlicer = new TowPicBitmapSlicer();
    private BitmapSlicer threePicBitmapSlicer = new ThreePicBitmapSlicer();
    private BitmapSlicer fourPicBitmapSlicer = new FourPicBitmapSlicer();
    private BitmapSlicer sixPicBitmapSlicer = new SixPicBitmapSlicer();
    private BitmapSlicer bitmapSlicer = ninePicBitmapSlicer;

    private List<Bitmap> lastDesBitmaps;


    private List<ImageView> ninePicImageViews = new ArrayList<>();
    private List<ImageView> towPickImageViews = new ArrayList<>();
    private List<ImageView> threePickImageViews = new ArrayList<>();
    private List<ImageView> fourPickImageViews = new ArrayList<>();
    private List<ImageView> sixPickImageViews = new ArrayList<>();
    private List<ImageView> currentImageViewList = ninePicImageViews;
    private View progressView;
    private View resultView;
    private TextView resultTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerForContextMenu(findViewById(R.id.btn_select));
        progressView = findViewById(R.id.layout_progress);
        resultView = findViewById(R.id.layout_result);
        resultTv = findViewById(R.id.tv_result);
        initImageViews();
        File baseDirFile = new File(baseDir);
        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断是否已经赋予权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {//这里可以写个对话框之类的项向用户解释为什么要申请权限，并在对话框的确认键后续再次申请权限
                    showToast("朕需要储存权限");
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("选择切图类型");
        menu.add(Menu.NONE, MENU_2_PIC, Menu.NONE, "二图");
        menu.add(Menu.NONE, MENU_3_PIC, Menu.NONE, "三图");
        menu.add(Menu.NONE, MENU_4_PIC, Menu.NONE, "四图");
        menu.add(Menu.NONE, MENU_6_PIC, Menu.NONE, "六图");
        menu.add(Menu.NONE, MENU_9_PIC, Menu.NONE, "九图");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_2_PIC:
                bitmapSlicer = towPicBitmapSlicer;
                currentImageViewList = towPickImageViews;
                break;
            case MENU_3_PIC:
                bitmapSlicer = threePicBitmapSlicer;
                currentImageViewList = threePickImageViews;
                break;
            case MENU_4_PIC:
                bitmapSlicer = fourPicBitmapSlicer;
                currentImageViewList = fourPickImageViews;
                break;
            case MENU_6_PIC:
                bitmapSlicer = sixPicBitmapSlicer;
                currentImageViewList = sixPickImageViews;
                break;
            case MENU_9_PIC:
                bitmapSlicer = ninePicBitmapSlicer;
                currentImageViewList = ninePicImageViews;
                break;

        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK);
        return true;
    }

    private void initImageViews() {
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image1));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image2));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image3));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image4));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image5));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image6));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image7));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image8));
        ninePicImageViews.add((ImageView) findViewById(R.id.iv_image9));

        towPickImageViews.add(ninePicImageViews.get(0));
        towPickImageViews.add(ninePicImageViews.get(1));

        threePickImageViews.add(ninePicImageViews.get(0));
        threePickImageViews.add(ninePicImageViews.get(1));
        threePickImageViews.add(ninePicImageViews.get(2));

        fourPickImageViews.add(ninePicImageViews.get(0));
        fourPickImageViews.add(ninePicImageViews.get(1));
        fourPickImageViews.add(ninePicImageViews.get(3));
        fourPickImageViews.add(ninePicImageViews.get(4));

        sixPickImageViews.add(ninePicImageViews.get(0));
        sixPickImageViews.add(ninePicImageViews.get(1));
        sixPickImageViews.add(ninePicImageViews.get(2));
        sixPickImageViews.add(ninePicImageViews.get(3));
        sixPickImageViews.add(ninePicImageViews.get(4));
        sixPickImageViews.add(ninePicImageViews.get(5));
    }

    public void choose(View v) {
        v.showContextMenu();
    }

    public void save(View v) {
        if (lastDesBitmaps == null) {
            showToast("请先选择图片");
            return;
        }
        progressView.setVisibility(View.VISIBLE);
        final File parent = new File(baseDir);
        final String prefix = System.currentTimeMillis() + "";
        final ArrayList<File> slices = new ArrayList<>();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        Observable.fromArray(lastDesBitmaps.toArray(new Bitmap[]{}))
                .map(new Function<Bitmap, File>() {
                    @Override
                    public File apply(Bitmap bitmap) throws Exception {
                        int index = lastDesBitmaps.indexOf(bitmap);
                        File file = new File(parent, prefix + "_" + (index + 1) + ".jpg");
                        OutputStream os = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        os.close();
                        return file;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        Uri uri = Uri.fromFile(file);
                        Log.d("xsm-save-files", uri.toString());
                        slices.add(file);
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "导出失败", Toast.LENGTH_SHORT).show();
                        progressView.setVisibility(View.GONE);
                        resultView.setVisibility(View.GONE);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        progressView.setVisibility(View.GONE);
                        resultView.setVisibility(View.VISIBLE);
                        resultTv.setText(Html.fromHtml("<font color=\"#868686\">切片已保存在</font><font color=\"#33a24e\">" + parent.getAbsolutePath() + "</font><font color=\"#868686\">，点击分享到朋友圈</font>"));
                        resultTv.setTag(slices);
                    }
                });
    }

    private Toast mToast;

    @UiThread
    private void showToast(final String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public void shareSlices(View v) {
        if (v.getTag() != null) {
            final ArrayList<File> slices = (ArrayList<File>) v.getTag();
            final ArrayList<Uri> sliceUris = new ArrayList<>();
            Observable.fromArray(slices.toArray(new File[]{}))
                    .map(new Function<File, Uri>() {
                        @Override
                        public Uri apply(File file) throws Exception {
                            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    , new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA}
                                    , MediaStore.Images.ImageColumns.DATA + "=?"
                                    , new String[]{file.getAbsolutePath()}
                                    , null);
                            if (cursor != null) {
                                if (cursor.getCount() != 0) {
                                    cursor.moveToFirst();
                                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                                    cursor.close();
                                    Log.d("xsm-read-media-database", "id = " + id + ", path = " + path);
                                    return Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + id);
                                } else {
                                    cursor.close();
                                    Log.w("xsm-read-media-database", "cursor is empty");
                                    return null;
                                }
                            } else {
                                Log.e("xsm-read-media-database", "cursor is null");
                                return null;
                            }
                        }
                    }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Uri>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            progressView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onNext(Uri uri) {
                            Log.d("xsm-collect-slice-uri", uri.toString());
                            sliceUris.add(uri);
                        }

                        @Override
                        public void onError(Throwable e) {
                            progressView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onComplete() {
                            Log.d("xsm-start-wechat", "start wechat with " + slices.size() + " pictures.");
                            progressView.setVisibility(View.GONE);
                            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.setType("image/jpeg");
                            ComponentName comp = new ComponentName("com.tencent.mm",
                                    "com.tencent.mm.ui.tools.ShareToTimeLineUI");
                            intent.setComponent(comp);
                            intent.setType("image/*");
                            intent.putExtra("Kdescription", "");
                            intent.putExtra(Intent.EXTRA_STREAM, sliceUris);
                            startActivity(intent);
                        }
                    });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK) {
                Uri uri = data.getData();
                int h = 0;
                int w = 0;
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    h = bitmap.getHeight();
                    w = bitmap.getWidth();
                    bitmap.recycle();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(uri, "image/*");
                // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
                intent.putExtra("crop", "true");
                //该参数可以不设定用来规定裁剪区的宽高比
                intent.putExtra("aspectX", bitmapSlicer.getAspectX());
                intent.putExtra("aspectY", bitmapSlicer.getAspectY());
                //该参数设定为你的imageView的大小
                intent.putExtra("outputX", bitmapSlicer.calculateOutputX(w, h));
                intent.putExtra("outputY", bitmapSlicer.calculateOutputY(w, h));
                //是否返回bitmap对象
                intent.putExtra("return-data", false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                intent.putExtra("noFaceDetection", true);
                startActivityForResult(intent, REQUEST_CODE_CUT);
            } else if (requestCode == REQUEST_CODE_CUT) {
                try {
                    InputStream is = new FileInputStream(tempFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    tempFile.delete();
                    bitmapSlicer.setSrcBitmap(bitmap)
                            .registerListener(bitmapSliceListener)
                            .slice();
                    progressView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                    progressView.setVisibility(View.GONE);
                    return;
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showToast("权限申请失败");
                    finish();
                    break;
                }
            }
        }
    }


}
