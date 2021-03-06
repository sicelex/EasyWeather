package com.example.huang.easyweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.huang.easyweather.adapter.ForecastAdapter;
import com.example.huang.easyweather.adapter.HourlyForecastAdapter;
import com.example.huang.easyweather.anim.AnimationManager;
import com.example.huang.easyweather.anim.RainView;
import com.example.huang.easyweather.anim.SnowView;
import com.example.huang.easyweather.gson.Forecast;
import com.example.huang.easyweather.gson.HourlyForecast;
import com.example.huang.easyweather.gson.Weather;
import com.example.huang.easyweather.service.AutoUpdateService;
import com.example.huang.easyweather.utilities.JudgeCond;
import com.example.huang.easyweather.utilities.NetworkUtils;
import com.example.huang.easyweather.utilities.WeatherJsonUtils;
import com.tencent.bugly.Bugly;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.huang.easyweather.utilities.JudgeCond.judgeCond;

public class WeatherActivity extends AppCompatActivity {

    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefresh;
    private String mCityId;
    private String mCityZh;
    private String mDegreeMax;
    private String mDegreeMin;
    private TextView cityBarText;
    private TextView titleCity;
    private TextView titleCond;
    private TextView titleUpdateTime;
    private TextView titleDegree;

    private TextView qltyText;
    private TextView aqiText;
    private TextView pm10Text;
    private TextView pm25Text;
    private TextView coText;
    private TextView no2Text;
    private TextView o3Text;
    private TextView so2Text;

    private TextView comfortText;
    private TextView carWashText;
    private TextView dressText;
    private TextView influenzaText;
    private TextView sportText;
    private TextView travelText;
    private TextView ultravioletText;

    private ImageView bgView;
    private ImageView imgView;
    private RainView rainView = null;
    private SnowView snowView=null;
    private ImageButton addCity;
    private List<HourlyForecast> hourlyForecastDatas=new ArrayList<>();//当前视图列表
    private RecyclerView hourlyRecyclerView;
    private HourlyForecastAdapter hourlyForecastAdapter;//创建适配器实例
    private List<Forecast> forecastDatas=new ArrayList<>();

    private RecyclerView forecastRecyclerView;
    private ForecastAdapter forecastAdapter;

    private boolean beforeWeatherInfo=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugly.init(this, "fb11b988bc", false);
        //沉浸式状态栏的兼容性配置
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //设置标题栏
        AppBarLayout appBarLayout=(AppBarLayout)findViewById(R.id.appBar);
//        CollapsingToolbarLayout collapsingToolbarLayout=(CollapsingToolbarLayout)
//                findViewById(R.id.collapsing_toolbar);
//        Toolbar toolbar=(Toolbar)findViewById(R.id.city_toolbar);
//        setSupportActionBar(toolbar);
        //防止下拉刷洗和折叠式标题栏冲突
        if(appBarLayout!=null){
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset ) {
                    if (verticalOffset >= 0) {
                        swipeRefresh.setEnabled(true);
                    } else {
                        swipeRefresh.setEnabled(false);
                    }
                }
            });
        }



//        //每日一图布局
//        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);
        //添加城市菜单
        addCity=(ImageButton) findViewById(R.id.add_city);
        addCity.getBackground().setAlpha(0);
        //下拉刷新
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        /*
        weather_title
         */
        //cityBarText=(TextView)findViewById(R.id.bar_city);
        titleCity=(TextView)findViewById(R.id.city) ;
        titleCond=(TextView)findViewById(R.id.cond) ;
        titleUpdateTime=(TextView)findViewById(R.id.update_time);
        titleDegree=(TextView)findViewById(R.id.degree);
        /*
        qlty
         */
        qltyText=(TextView)findViewById(R.id.qlty_text);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm10Text=(TextView)findViewById(R.id.pm10_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        coText=(TextView)findViewById(R.id.co_text);
        no2Text=(TextView)findViewById(R.id.no2_text);
        o3Text=(TextView)findViewById(R.id.o3_text);
        so2Text=(TextView)findViewById(R.id.so2_text);
        /*
        suggestion
         */
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        dressText=(TextView)findViewById(R.id.drsg_text);
        influenzaText=(TextView)findViewById(R.id.flu_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        travelText=(TextView)findViewById(R.id.trav_text);
        ultravioletText=(TextView)findViewById(R.id.uv_text);

        /*
        天气动画
         */
        bgView=(ImageView)findViewById(R.id.bg);
        imgView=(ImageView)findViewById(R.id.img);
        rainView=(RainView)findViewById(R.id.rain) ;
        snowView=(SnowView)findViewById(R.id.snow) ;


        /*
        HourlyForecastAdapter
         */
        hourlyRecyclerView=(RecyclerView)findViewById(R.id.hourly_recyclerView);
        LinearLayoutManager HourlyLayoutManager=new LinearLayoutManager(this);//创建布局管理器的实例
        HourlyLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);//设置布局方式为水平
        hourlyRecyclerView.setLayoutManager(HourlyLayoutManager);//设置布局
        hourlyForecastAdapter=new HourlyForecastAdapter(hourlyForecastDatas);//创建适配器实例
        hourlyRecyclerView.setAdapter(hourlyForecastAdapter);//设置适配器
        hourlyRecyclerView.setHasFixedSize(true);//保持item的宽或高不会变

        /*
        ForecastAdapter
         */
        forecastRecyclerView=(RecyclerView)findViewById(R.id.forecast_recyclerView);
        LinearLayoutManager forecastLayoutManager=new LinearLayoutManager(this);
        forecastRecyclerView.setLayoutManager(forecastLayoutManager);
        forecastAdapter=new ForecastAdapter(forecastDatas);
        forecastRecyclerView.setAdapter(forecastAdapter);
        forecastRecyclerView.setHasFixedSize(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        Weather weather = WeatherJsonUtils.handleWeatherResponse(weatherString);


        if (weatherString != null && beforeWeatherInfo) {
             //有缓存时直接解析天气数据

            mCityId=weather.basic.cityId;
            mCityZh=weather.basic.cityZh;
            beforeWeatherInfo=false;
            Log.d("CityManager","有缓存");
            mCityId = getIntent().getStringExtra("city_id");

            requestWeather(mCityId);
            showWeatherInfo(weather);


        } else {
             //无缓存时去服务器查询天气
            mCityId = getIntent().getStringExtra("city_id");
            requestWeather(mCityId);
            Log.d("CityManager","无缓存");
        }
        mCityId = getIntent().getStringExtra("city_id");
        requestWeather(mCityId);

        //添加城市按钮
        addCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String weatherString = prefs.getString("weather", null);
                Weather weather = WeatherJsonUtils.handleWeatherResponse(weatherString);
               sendCityAndDegree(weather);

            }
        });
//        appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
//            @Override
//            public void onStateChanged(AppBarLayout appBarLayout, State state) {
//                Log.d("STATE", state.name());
//                if( state == State.EXPANDED ) {
//
//                    //展开状态
//                    animation.start();
//                    Toast.makeText(WeatherActivity.this, "动画开启", Toast.LENGTH_SHORT).show();
//
//                }else if(state == State.COLLAPSED){
//
//                    //折叠状态
//                    //getSupportActionBar().setTitle("bei");
//                    animation.cancel();
//                    Toast.makeText(WeatherActivity.this, "动画关闭", Toast.LENGTH_SHORT).show();
//                    //cityBarText.setText("nihao");
//
//                }else {
//
//                    //中间状态
//                    animation.start();
//
//                }
//            }
//        });
        //下拉刷新
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //mCityId = getIntent().getStringExtra("city_id");
                requestWeather(mCityId);
            }
        });
        //若有图片缓存，加载，若无，重新请求
//        String bingPic = prefs.getString("bing_pic", null);
//        if (bingPic != null) {
//            Glide.with(this).load(bingPic).into(bingPicImg);
//        } else {
//            loadBingPic();
//        }
    }
    /*
     根据天气id请求天气数据
     */
    public void requestWeather(final String cityId){
        String weatherUrl="https://free-api.heweather.com/v5/weather?city="+cityId+"&key=a944761a388842c1af1c47f8a16b95c7";
        NetworkUtils.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String returnWeatherInfo= response.body().string();
                final Weather weather=WeatherJsonUtils.handleWeatherResponse(returnWeatherInfo);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor= PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this)
                                    .edit();
                            editor.putString("weather",returnWeatherInfo);
                            editor.apply();
                            mCityId=weather.basic.cityId;
                            showWeatherInfo(weather);
                            Intent intent=new Intent(WeatherActivity.this, AutoUpdateService.class);
                            startService(intent);
                        }
//                        else {
//                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
//                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }
    private void sendCityAndDegree(Weather weather){
        mCityId=weather.basic.cityId;
        mCityZh=weather.basic.cityZh;
        mDegreeMax=weather.forecastList.get(0).temperature.max+"°";
        mDegreeMin=weather.forecastList.get(0).temperature.min+"°";
        Intent intent=new Intent(WeatherActivity.this,CityManager.class);
        intent.putExtra("city_id",mCityId);
        intent.putExtra("city_zh",mCityZh);
        intent.putExtra("degree_max",mDegreeMax);
        intent.putExtra("degree_min",mDegreeMin);
        Log.d("CityManager","要传出去的城市和温度："+mCityId+"\t"+mCityZh+"\t"+mDegreeMax+"\t"+mDegreeMin);
        startActivity(intent);
    }

    public void showWeatherInfo(Weather weather){
        String cityZh=weather.basic.cityZh;//城市中文名称
        String cond=weather.now.more.info;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];//更新时间
        String degree=weather.now.temperature+"℃";//当前天气温度
        judgeCond(cond,bgView,imgView,rainView,snowView);//判断当前天气并加载相应的动画
        titleCity.setText(cityZh);
        titleCond.setText(cond);
        titleUpdateTime.setText(updateTime);
        titleDegree.setText(degree);
        //加载每小时预报数据
        hourlyForecastDatas.clear();
        for(HourlyForecast hourlyForecast:weather.hourlyForecastList){
            hourlyForecastDatas.add(hourlyForecast);
        }
        hourlyForecastAdapter.notifyDataSetChanged();
        //加载未来天气预报数据
        forecastDatas.clear();
        for(Forecast forecast:weather.forecastList){
            forecastDatas.add(forecast);
        }
        forecastAdapter.notifyDataSetChanged();
        //加载空气质量数据
        if(weather.aqi==null){
            qltyText.setText("空气等级 暂无");
            aqiText.setText("指数 暂无");
            pm10Text.setText("暂无");
            pm25Text.setText("暂无");
            coText.setText("暂无");
            no2Text.setText("暂无");
            o3Text.setText("暂无");
            so2Text.setText("暂无");
        }else{
            String qlty=weather.aqi.city.qlty;
            String aqi=weather.aqi.city.aqi;
            String pm10=weather.aqi.city.pm10;
            String pm25=weather.aqi.city.pm25;
            String co=weather.aqi.city.co;
            String no2=weather.aqi.city.no2;
            String o3=weather.aqi.city.o3;
            String so2=weather.aqi.city.so2;
            Log.d("WeatherActivity",co+"\t"+no2+"\t"+o3+"\t"+so2);
            if(qlty==null)qltyText.setText("暂无");else qltyText.setText(qlty);
            if(aqi==null) aqiText.setText("暂无");else aqiText.setText("指数 "+aqi);
            if(pm10==null)pm10Text.setText("暂无");else pm10Text.setText(pm10);
            if(pm25==null) pm25Text.setText("暂无");else pm25Text.setText(pm25);
            if(co==null) coText.setText("暂无");else  coText.setText(co);
            if(no2==null) no2Text.setText("暂无");else   no2Text.setText(no2);
            if(o3==null)o3Text.setText("暂无");else o3Text.setText(o3);
            if(so2==null)so2Text.setText("暂无");else  so2Text.setText(so2);
        }
        //加载生活建议数据
        String comfort = "舒适度：" + weather.suggestion.comfort.brief+"\n" +weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.brief+"\n"+weather.suggestion.carWash.info;
        String dress = "穿衣指数：" + weather.suggestion.dress.brief+"\n"+weather.suggestion.dress.info;
        String influenza = "感冒指数：" + weather.suggestion.dress.brief+"\n"+weather.suggestion.influenza.info;
        String sport = "运动指数：" + weather.suggestion.sport.brief+"\n"+weather.suggestion.sport.info;
        String travel = "旅行指数：" + weather.suggestion.travel.brief+"\n"+weather.suggestion.travel.info;
        String ultraviolet = "紫外线指数：" +weather.suggestion.ultraviolet.brief+"\n"+ weather.suggestion.ultraviolet.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        dressText.setText(dress);
        influenzaText.setText(influenza);
        sportText.setText(sport);
        travelText.setText(travel);
        ultravioletText.setText(ultraviolet);
    }
}
