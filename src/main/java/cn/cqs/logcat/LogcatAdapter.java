package cn.cqs.logcat;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class LogcatAdapter extends BaseAdapter implements Filterable {

    private ArrayList<LogItem> mData;
    @Nullable private ArrayList<LogItem> mFilteredData = null;
    //日志级别类型过滤器
    @Nullable private String mLevelFilter = null;
    //日志TAG过滤器
    @Nullable private String mTagFilter = null;
    private String[] spinnerArray = null;
    public LogcatAdapter(Context context) {
        mData = new ArrayList<>();
        spinnerArray = context.getResources().getStringArray(R.array.logcat_spinner);
    }

    public void append(LogItem item) {
        synchronized (LogcatAdapter.class) {
            mData.add(item);
            if (mLevelFilter != null && mFilteredData != null) {
                if (mTagFilter != null){
                    if (!item.isFiltered(mLevelFilter) && item.tag.equals(mTagFilter)) {
                        mFilteredData.add(item);
                    }
                } else {
                    if (!item.isFiltered(mLevelFilter)) {
                        mFilteredData.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public void clear() {
        synchronized (LogcatAdapter.class) {
            mData.clear();
            mTagFilter = null;
            mLevelFilter = null;
            mFilteredData = null;
            notifyDataSetChanged();
        }
    }

    public LogItem[] getData() {
        synchronized (LogcatAdapter.class) {
            return mData.toArray(new LogItem[mData.size()]);
        }
    }

    @Override
    public int getCount() {
        return mFilteredData != null ? mFilteredData.size() : mData.size();
    }

    @Override
    public LogItem getItem(int position) {
        return mFilteredData != null ? mFilteredData.get(position) : mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = convertView;
        Holder holder;
        if (item == null) {
            item = LayoutInflater.from(parent.getContext()).inflate(R.layout.logcat_list_item, parent, false);
            holder = new Holder(item);
        } else {
            holder = (Holder) item.getTag();
        }
        holder.parse(getItem(position));
        return item;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                synchronized (LogcatAdapter.class) {
                    FilterResults results = new FilterResults();
                    ArrayList<LogItem> filtered = new ArrayList<>();
                    if (!TextUtils.isEmpty(constraint)){
                        if (contain(spinnerArray, (String) constraint)){
                            mLevelFilter = String.valueOf(constraint.charAt(0));
                        } else {
                            mTagFilter = (String) constraint;
                        }
                        for (LogItem item : mData) {
                            if (mLevelFilter != null && mTagFilter != null){
                                if (!item.isFiltered(mLevelFilter) && item.tag.equals(mTagFilter)) {
                                    filtered.add(item);
                                }
                            } else {
                                if (mTagFilter == null){
                                    if (!item.isFiltered(mLevelFilter)) {
                                        filtered.add(item);
                                    }
                                }
                            }
                        }
                    } else {
                        mTagFilter = null;
                        for (LogItem item : mData) {
                            if (!item.isFiltered(mLevelFilter)) {
                                filtered.add(item);
                            }
                        }
                    }
                    results.values = filtered;
                    results.count = filtered.size();
                    return results;
                }
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values == null) {
                    mFilteredData = null;
                } else {
                    //noinspection unchecked
                    mFilteredData = (ArrayList<LogItem>) results.values;
                }
                notifyDataSetChanged();
            }
        };
    }


//    public static boolean contain(String[] arr, String targetValue) {
//        Log.e("TAG","spinnerArray：" + TextUtils.join("|",arr));
//        int a = Arrays.binarySearch(arr, targetValue);
//        return a > 0;
//    }
    /**
     * 判断数组是否包含目标值
     * @param arr
     * @param targetValue
     * @return
     */
    public boolean contain(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue))
                return true;
        }
        return false;
    }
    public static class Holder {
        private static final SimpleDateFormat sDateFormat = new SimpleDateFormat(
                "MM-dd hh:mm:ss.SSS", Locale.getDefault());
        TextView tag;
        TextView time;
        TextView content;

        Holder(View item) {
            tag = item.findViewById(R.id.tv_tag);
            time = item.findViewById(R.id.tv_time);
            content = item.findViewById(R.id.tv_content);
            item.setTag(this);
        }

        void parse(LogItem data) {
            time.setText(String.format(Locale.getDefault(),"%s %d-%d/%s",
                    sDateFormat.format(data.time), data.processId, data.threadId, data.tag));
            content.setText(data.content);
            tag.setText(data.priority);
            tag.setBackgroundResource(data.getColorRes());
        }
    }
}
