package cn.cqs.logcat;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.Snackbar;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 日志打印窗口
 */
public class LogcatDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private BottomSheetBehavior mBehavior;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.logcat_bottom_dialog, null);
        initView(view);
        dialog.setContentView(view);
        mBehavior = BottomSheetBehavior.from((View) view.getParent());
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        //0.8屏幕高度
        int height = (int) (dialog.getContext().getResources().getDisplayMetrics().heightPixels * 0.8);
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
        mBehavior.setPeekHeight(height);
        dialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
    private EditText searchEt;
    private Spinner spinner;
    private ImageView clearImageView,shareImageView,searchImageView;
    /*当前状态*/
    private TextView playStateTv;
    private ListView listView;
    private LogcatAdapter logcatAdapter;
    @SuppressLint("ClickableViewAccessibility")
    private void initView(View view) {
        searchEt = view.findViewById(R.id.search_input);
        searchImageView = view.findViewById(R.id.logcat_search);
        spinner = view.findViewById(R.id.spinner);
        clearImageView = view.findViewById(R.id.iv_clean);
        shareImageView = view.findViewById(R.id.iv_share);
        playStateTv = view.findViewById(R.id.tv_pause);
        listView = view.findViewById(R.id.listView);
        logcatAdapter = new LogcatAdapter(getContext());
        searchImageView.setOnClickListener(this);
        clearImageView.setOnClickListener(this);
        shareImageView.setOnClickListener(this);
        playStateTv.setOnClickListener(this);
        initSpinnerStyle(spinner,getResources().getStringArray(R.array.logcat_spinner));
        //搜索监听
        searchEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == 0 || actionId == 3) && event != null) {
                    String searchValue = searchEt.getText().toString().trim();
                    logcatAdapter.getFilter().filter(searchValue);
                }
                return false;
            }
        });
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setStackFromBottom(true);
        listView.setAdapter(logcatAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogItem item = logcatAdapter.getItem(position);
                LogcatDetailActivity.launch(getContext(),item);
            }
        });
        //解决BottomSheetDialog + Listview出现的滑动冲突问题
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
                if (!listView.canScrollVertically(-1)) {
                    listView.requestDisallowInterceptTouchEvent(false);
                }else{
                    listView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
        startLogcatThread();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        destroy = true;

    }

    @Override
    public void onStart() {
        super.onStart();
        //默认全屏展开
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void hide(View v) {
        //点击任意布局关闭
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
    /**
     * Spinner统一风格
     *
     * @param items
     * @param spinner
     */
    public void initSpinnerStyle(Spinner spinner, String[] items) {
        setSpinnerDropDownVerticalOffset(spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(spinner.getContext(), R.layout.logcat_dropdown_item, R.id.spinner_item, items);
        adapter.setDropDownViewResource(R.layout.logcat_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String filter = getResources().getStringArray(R.array.logcat_spinner)[position];
                logcatAdapter.getFilter().filter(filter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置系统Spinner的下拉偏移
     *
     * @param spinner
     */
    public void setSpinnerDropDownVerticalOffset(Spinner spinner) {
        int itemHeight = dip2px(getContext(),30f);
        int dropdownOffset = dip2px(getContext(),1f);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            spinner.setDropDownVerticalOffset(0);
        } else {
            spinner.setDropDownVerticalOffset(itemHeight + dropdownOffset);
        }
    }
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    /**
     * 日志状态控制
     */
    private boolean isStop = false;
    /**
     * 销毁线程
     */
    private boolean destroy =  false;

    private void startLogcatThread() {
        new Thread() {
            @Override
            public void run() {
                BufferedReader reader = null;
                try {
                    Process process = new ProcessBuilder("logcat", "-v", "threadtime").start();
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (!destroy && (line = reader.readLine()) != null) {
                        if (LogItem.IGNORED_LOG.contains(line)) {
                            continue;
                        }
                        if(!isStop){
                            final LogItem item = new LogItem(line);
                            listView.post(new Runnable() {
                                @Override
                                public void run() {
                                    logcatAdapter.append(item);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopLogcat();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopLogcat();
                    }
                }
            }
        }.start();
    }

    @Override
    public void onClick(final View v) {
        int id = v.getId();
        if (id == R.id.iv_clean) {
            logcatAdapter.clear();
            spinner.setSelection(0);
            searchEt.setText(null);
        } else if (id == R.id.iv_share) {
            @SuppressLint("StaticFieldLeak")
            ExportLogFileTask task = new ExportLogFileTask(getContext().getExternalCacheDir()) {
                @Override
                protected void onPostExecute(File file) {
                    if (file == null) {
                        Snackbar.make(v, R.string.logcat_create_log_file_failed, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        shareIntent.setType("text/plain");
                        Uri uri = LogcatFileProvider.getUriForFile(getContext().getApplicationContext(),
                                getContext().getPackageName() + ".logcat_provider", file);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        if (getContext().getPackageManager().queryIntentActivities(shareIntent, 0).isEmpty()) {
                            Snackbar.make(v, R.string.logcat_not_support_on_this_device, Snackbar.LENGTH_SHORT).show();
                        } else {
                            startActivity(shareIntent);
                        }
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, logcatAdapter.getData());
        } else if (id == R.id.tv_pause) {
            if (isStop){
                startLogcat();
            } else {
                stopLogcat();
            }
        } else if (id == R.id.logcat_search){
            String searchValue = searchEt.getText().toString().trim();
            logcatAdapter.getFilter().filter(searchValue);
        }
    }

    private void startLogcat(){
        isStop = false;
        playStateTv.setText("暂停");
    }
    private void stopLogcat(){
        isStop = true;
        playStateTv.setText("开始");
    }
}