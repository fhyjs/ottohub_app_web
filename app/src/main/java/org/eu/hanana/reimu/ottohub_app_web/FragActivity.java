package org.eu.hanana.reimu.ottohub_app_web;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;


public class FragActivity extends AppCompatActivity {
    public static final String ARG_FRAG_CLASS = "class";
    public static final String ARG_DATA = "data";
    public static final String ARG_TITLE = "title";
    public static Intent getIntent(Context context, Bundle fragData, Class<? extends Fragment> clazz, String title){
        var intent = new Intent(context, FragActivity.class);
        var data = new Bundle();
        data.putString(FragActivity.ARG_FRAG_CLASS, clazz.getName());
        data.putString(FragActivity.ARG_TITLE, title);
        data.putBundle(FragActivity.ARG_DATA,fragData);
        intent.putExtras(data);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_frag);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 启用返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Fragment fragment;
        try {
            fragment = (Fragment) Class.forName(getIntent().getStringExtra(ARG_FRAG_CLASS)).newInstance();
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        fragment.setArguments(getIntent().getBundleExtra(ARG_DATA));
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
        if (getIntent().getExtras().containsKey(ARG_TITLE)){
            var title = getIntent().getStringExtra(ARG_TITLE);
            toolbar.setTitle(title);
        }else {
            toolbar.setTitle(getIntent().getStringExtra(ARG_FRAG_CLASS));
        }
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 默认返回栈顶页面
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}