package com.alsa.picturecut;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alsa.picturecut.view.PictureCutView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * EditActivity [ 裁剪图片 ]
 * created by alsa on 2019/12/11
 */
public class EditActivity extends AppCompatActivity {

    @BindView(R.id.container)
    FrameLayout container;

    /**
     * 自定义图像剪裁View
     */
    private PictureCutView pictureCutView;

    /**
     * ButterKnife绑定对象
     */
    private Unbinder unbinder;

    /**
     * 从相册选取的图片绝对路径
     */
    private String mPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        unbinder = ButterKnife.bind(this);

        initVariables();

        // 绘制图像
        pictureCutView = new PictureCutView(this);
        pictureCutView.setPhotoPath(mPhotoPath);
        // 设置可点击，否则接收不到MOVE事件
        pictureCutView.setClickable(true);
        container.addView(pictureCutView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        pictureCutView.setActionBarHeight(getSupportActionBar().getHeight());
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_ok) {
            pictureCutView.cutPicure(1);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 初始化变量
     */
    private void initVariables() {
        Bundle args = getIntent().getExtras();
        if (args != null) {
            mPhotoPath = args.getString("path");
        }
    }
}
