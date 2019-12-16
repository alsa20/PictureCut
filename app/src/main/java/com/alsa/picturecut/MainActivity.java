package com.alsa.picturecut;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alsa.library.PermissionManager;
import com.alsa.library.listener.PermissionCallback;
import com.alsa.picturecut.utils.AlbumUtil;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * MainActivity [ 项目入口 ]
 * created by alsa on 2019/12/11 0011
 */
public class MainActivity extends AppCompatActivity implements PermissionCallback {

    /**
     * 读写权限
     */
    public static final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * 权限申请的请求码
     */
    public static final int WRITE_PERMISSION_REQUEST_CODE = 101;

    /**
     * 打开相册的请求码
     */
    public static final int GALLERY_REQUEST_CODE = 102;

    /**
     * ButterKnife对象，解绑时需要
     */
    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @OnClick(R.id.button)
    void openAlbum() {
        // 申请权限
        PermissionManager.requestPermissions(this, WRITE_PERMISSION_REQUEST_CODE, permissions);
    }

    @Override
    public void onPermissionGranted(int requestCode, List<String> permissions) {
        // 打开相册
        AlbumUtil.openPhotoAlbum(this, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onPermissionDenied(int requestCode, List<String> permissions) {
        // 打开提示框
        PermissionManager.openSettingDialog(this, permissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 使用此方法处理权限，调用onPermissionGranted()和onPermissionDenied()
        PermissionManager.onRequestPermissionResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE) {
            // 如果已选择图片，打开编辑页面
            if (data.getData() != null) {
                String photoPath = AlbumUtil.getRealPathFromUri(this, data.getData());
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("path", photoPath);
                startActivity(intent);
            }
        }
    }
}
