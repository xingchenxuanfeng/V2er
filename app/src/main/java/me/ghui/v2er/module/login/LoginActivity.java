package me.ghui.v2er.module.login;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import me.ghui.v2er.general.App;
import me.ghui.v2er.network.APIService;
import me.ghui.v2er.util.Check;
import me.ghui.v2er.util.Theme;
import me.ghui.v2er.R;
import me.ghui.v2er.general.ActivityReloader;
import me.ghui.v2er.general.GlideApp;
import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.general.Vtml;
import me.ghui.v2er.injector.component.DaggerLoginComponent;
import me.ghui.v2er.injector.module.LoginModule;
import me.ghui.v2er.module.base.BaseActivity;
import me.ghui.v2er.module.home.MainActivity;
import me.ghui.v2er.module.settings.UserManualActivity;
import me.ghui.v2er.network.bean.LoginParam;
import me.ghui.v2er.util.L;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.util.Voast;
import me.ghui.v2er.widget.BaseToolBar;
import me.ghui.v2er.widget.dialog.ConfirmDialog;

/**
 * Created by ghui on 30/04/2017.
 */

public class LoginActivity extends BaseActivity<LoginContract.IPresenter> implements LoginContract.IView {
    @BindView(R.id.login_user_name_text_input_layout)
    TextInputLayout mUserInputLayout;
    @BindView(R.id.login_psw_text_input_layout)
    TextInputLayout mPswInputLayout;
    @BindView(R.id.login_go_btn)
    Button mLoginBtn;
    @BindView(R.id.captcha_img)
    ImageView mCaptchaImg;
    @BindView(R.id.login_captcha_text_input_layout)
    TextInputLayout mCaptchaInputLayout;
    @BindView(R.id.capcha_wrapper)
    ViewGroup mCaptchaWrapper;
    @BindView(R.id.img_loading_view)
    ProgressBar mImgLoadingView;
    @BindView(R.id.login_by_google_btn)
    Button mGoogleLoginBtn;

    //登录参数加载成功标识
    private boolean mHasLoaded;
    private LoginParam mLoginParam;

    public static boolean sIsLoginActivityResume = false;

    public static void goLogin() {
        if (!sIsLoginActivityResume) {
            Navigator.from(App.get()).to(LoginActivity.class).addFlag(FLAG_ACTIVITY_NEW_TASK).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sIsLoginActivityResume = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        sIsLoginActivityResume = false;
    }

    @Override
    protected void startInject() {
        DaggerLoginComponent.builder()
                .appComponent(getAppComponent())
                .loginModule(new LoginModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void init() {
        super.init();
        Utils.setPaddingForNavbar(mRootView);

        mGoogleLoginBtn.setVisibility(View.GONE);
        checkGoogleAccess(new Runnable() {
            @Override
            public void run() {
                mGoogleLoginBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    public static void checkGoogleAccess(Runnable runnable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = canAccessGoogle();
                Log.d("LoginActivity", "checkGoogleAccess,result:" + result);
                if (result) {
                    new Handler(Looper.getMainLooper()).post(runnable);
                }
            }
        }).start();
    }

    public static boolean canAccessGoogle() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 www.google.com");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            Log.e("LoginActivity", "canAccessGoogle", e);
        }
        return false;
    }

    @Override
    protected void reloadMode(int mode) {
        ActivityReloader.target(this).reload();
    }

    @Override
    protected void configToolBar(BaseToolBar toolBar) {
        super.configToolBar(toolBar);
        Utils.setPaddingForStatusBar(toolBar);
        toolBar.setElevation(0);
        toolBar.inflateMenu(R.menu.login_toolbar_menu);
        toolBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_register) {
                Utils.openInBrowser(APIService.getImprBaseUrl() + "/signup?r=ghui", this);
            } else if (item.getItemId() == R.id.action_forgot_psw) {
                Utils.openInBrowser(APIService.getImprBaseUrl() + "/forgot", this);
            } else if (item.getItemId() == R.id.action_faq) {
                Navigator.from(this).to(UserManualActivity.class).start();
            } else if (item.getItemId() == R.id.action_about) {
                Utils.openInBrowser(APIService.getImprBaseUrl() + "/about", this);
            }
            return true;
        });
    }

    @Override
    protected int attachLayoutRes() {
        return R.layout.act_login;
    }

    @Override
    protected void autoLoad() {
        mPresenter.start();
    }

    @OnClick(R.id.captcha_img_wrapper)
    void onCatchaClicked() {
        mCaptchaImg.setVisibility(View.INVISIBLE);
        mImgLoadingView.setVisibility(View.VISIBLE);
        mPresenter.start();
    }

    @Override
    protected int pageColor() {
        return Theme.getColor(R.attr.colorPrimary, this);
    }

    @OnClick(R.id.login_go_btn)
    void onLoginClicked() {
        String userName = mUserInputLayout.getEditText().getText().toString();
        String psw = mPswInputLayout.getEditText().getText().toString();
        String captcha = mCaptchaInputLayout.getEditText().getText().toString();
        if (Check.isEmpty(userName)) {
            mUserInputLayout.setError("请输入用户名");
            return;
        }

        if (Check.isEmpty(psw)) {
            mPswInputLayout.setError("请输入密码");
            return;
        }
        if (!mHasLoaded) {
            toast("登录参数正在加载，请稍后...");
            return;
        }

        if (mCaptchaWrapper.getVisibility() == View.VISIBLE && Check.isEmpty(captcha)) {
            mCaptchaInputLayout.setError("请输入验证码");
            return;
        }

        mPresenter.login(userName, psw, captcha);
    }

    @OnClick(R.id.login_by_google_btn)
    void onSignInWithGoogleClicked() {
        if (!mHasLoaded) {
            toast("登录参数正在加载，请稍后...");
            return;
        }
        mPresenter.signInWithGoogle();
    }

    @Override
    public void onFetchLoginParamFailure() {
        mHasLoaded = false;
        toast("加载登录参数出错");
        mLoginParam = null;
        new ConfirmDialog.Builder(getActivity())
                .title("加载登录参数出错")
                .msg("是否重试")
                .negativeText(R.string.cancel)
                .positiveText(R.string.ok, dialog -> autoLoad()).build().show();
    }

    @Override
    public void onFetchLoginParamSuccess(LoginParam loginParam) {
        L.d("加载登录参数成功");
        mLoginParam = loginParam;
        if (mLoginParam.needCaptcha()) {
            String capchaUrl = APIService.getImprBaseUrl() + "/_captcha?once=" + loginParam.getOnce();
            GlideApp.with(this).
                    load(capchaUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Voast.show("验证码加载失败");
                            mImgLoadingView.setVisibility(View.INVISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            mImgLoadingView.setVisibility(View.INVISIBLE);
                            mCaptchaImg.setVisibility(View.VISIBLE);
                            return false;
                        }
                    })
                    .into(mCaptchaImg);
        }
        mHasLoaded = true;
    }

    @Override
    public void onLoginSuccess() {
        toast("登录成功");
        Navigator.from(this)
                .to(MainActivity.class)
                .setFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .start();
        finish();
    }

    @Override
    public void onOccuredTwoStep() {
        mPresenter.start();
        mHasLoaded = false;
    }

    @Override
    public void onLoginFailure(String msg, boolean withProblem) {
        if (!withProblem) {
            toast(msg);
        } else {
            new ConfirmDialog.Builder(this)
                    .msg(Vtml.fromHtml(msg))
                    .positiveText("确定", dialog -> {
                        try {
                            mCaptchaInputLayout.getEditText().setText(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // auto refresh
                        onCatchaClicked();
                    })
                    .negativeText("取消", dialog -> finish())
                    .build().show();
        }
    }
}
