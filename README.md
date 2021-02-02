# logcat
本地在线logcat显示工具，方便某些机器不方便连接Android Studio或者wifi调试的情况
#### 使用
```gradle
  //添加依赖
  implementation 'com.github.bingoloves:logcat:1.0.0'
```
```java
  //这里配合悬浮框debug模式配置
   FloatViewController.getInstance().init(BuildConfig.DEBUG, this, R.drawable.ic_debug, new FloatView.OnFloatViewIconClickListener() {
        @Override
        public void onFloatViewClick(Context context) {
            if (context instanceof FragmentActivity){
                 //设置点击悬浮框事件，这里打开logcat(重点)
                 new LogcatDialog().show(((FragmentActivity)context).getSupportFragmentManager(),"logcat");
            }
        }
   });
   //显示悬浮框
   FloatViewController.getInstance().show(this);
   //隐藏悬浮框
   FloatViewController.getInstance().dismiss(this);
```