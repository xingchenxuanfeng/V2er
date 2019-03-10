package me.ghui.v2er.module.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.orhanobut.logger.Logger;
import com.trello.rxlifecycle2.LifecycleTransformer;

import java.util.Stack;

import javax.inject.Inject;

import butterknife.ButterKnife;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import io.reactivex.ObservableTransformer;
import me.ghui.toolbox.android.Check;
import me.ghui.toolbox.android.Theme;
import me.ghui.v2er.R;
import me.ghui.v2er.general.App;
import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.injector.component.AppComponent;
import me.ghui.v2er.module.home.MainActivity;
import me.ghui.v2er.module.login.LoginActivity;
import me.ghui.v2er.module.login.TwoStepLoginActivity;
import me.ghui.v2er.network.APIService;
import me.ghui.v2er.network.GeneralError;
import me.ghui.v2er.network.ResultCode;
import me.ghui.v2er.network.bean.TwoStepLoginInfo;
import me.ghui.v2er.util.DayNightUtil;
import me.ghui.v2er.util.RxUtils;
import me.ghui.v2er.util.UserUtils;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.util.Voast;
import me.ghui.v2er.widget.BaseToolBar;
import me.ghui.v2er.widget.V2erPtrFrameLayout;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by ghui on 05/03/2017.
 */

public abstract class BaseActivity<T extends BaseContract.IPresenter> extends RxActivity implements BaseContract.IView,
        IBindToLife, IBackHandler, BaseToolBar.OnDoubleTapListener {

    protected FrameLayout mRootView;
    protected ViewGroup mContentView;
    @Nullable
    protected BaseToolBar mToolbar;
    protected AppBarLayout mToolbarWrapper;
    protected View mLoadingView;

    @Inject
    public T mPresenter;
    private Stack<IBackable> mBackables;
    public static long FIRST_LOADING_DELAY = 100;
    private long mFirstLoadingDelay = FIRST_LOADING_DELAY;
    private Runnable mDelayLoadingRunnable;

    public void setFirstLoadingDelay(long delay) {
        mFirstLoadingDelay = delay;
    }

    /**
     * bind a layout resID to the content of this page
     *
     * @return layout res id
     */
    @LayoutRes
    protected abstract int attachLayoutRes();

    /**
     * Set a default Toolbar, if you don't want certain page to have a toolbar,
     * just return null;
     * The toolbar must has a id and value is : inner_toolbar
     *
     * @return
     */
    protected BaseToolBar attachToolbar() {
        int layoutId = attachToolBar() == 0 ? R.layout.appbar_wrapper_toolbar : attachToolBar();
        mToolbarWrapper = (AppBarLayout) getLayoutInflater().inflate(layoutId, null);
        return (BaseToolBar) mToolbarWrapper.findViewById(R.id.inner_toolbar);
    }

    @LayoutRes
    protected int attachToolBar() {
        return 0;
    }

    /**
     * config toolbar here
     */
    protected void configToolBar(BaseToolBar toolBar) {
        if (toolBar == null) return;
        toolBar.setTitle(getTitle());
        toolBar.setNavigationOnClickListener(view -> {
            if (isTaskRoot()) finishToHome();
            else onBackPressed();
        });
        toolBar.setOnDoubleTapListener(this);
    }

    @Override
    public boolean onToolbarDoubleTaped() {
        RecyclerView recyclerView = $(R.id.base_recyclerview);
        if (recyclerView != null) {
//            recyclerView.smoothScrollToPosition(0);
            recyclerView.scrollToPosition(0);
            return true;
        }
        return false;
    }

    protected void setTitle(String title, String subTitle) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
            mToolbar.setSubtitle(subTitle);
        }
    }

    protected void setTitle(String title) {
        setTitle(title, null);
    }

    /**
     * if you want to support ptr, then attach a PtrHandler to it
     *
     * @return PtrHandler
     */
    protected PtrHandler attachPtrHandler() {
        return null;
    }


    @Override
    public void handleBackable(IBackable backable) {
        if (mBackables == null) {
            mBackables = new Stack<>();
        }
        if (!mBackables.contains(backable)) {
            mBackables.push(backable);
        }
    }

    @Override
    public IBackable popBackable(IBackable backable) {
        if (mBackables != null && mBackables.contains(backable)) {
            mBackables.remove(backable);
        }
        return backable;
    }

    @Override
    public void onBackPressed() {
        if (Check.notEmpty(mBackables)) {
            mBackables.pop().onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isBackableEmpty() {
        return Check.isEmpty(mBackables);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackables != null) {
            mBackables.clear();
        }
    }

    /**
     * push a fragment to the fragment stack
     *
     * @param fragment
     */
    public void pushFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(mRootView.getId(), fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * just add a fragment to the top of the activity, unbackable
     *
     * @param fragment
     */
    public void addFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(mRootView.getId(), fragment)
                .commit();
    }

    /**
     * init Dagger2 injector
     */
    protected void startInject() {

    }

    /**
     * parse the intent extra data
     */
    protected void parseExtras(Intent intent) {

    }

    /**
     * init views in this page
     */
    protected void init() {

    }

    /**
     * init theme here
     */
    protected void initTheme() {
        switch (DayNightUtil.getMode()) {
            case DayNightUtil.NIGHT_MODE:
                setTheme(R.style.NightTheme);
                break;
            case DayNightUtil.AUTO_MODE:
                break;
            case DayNightUtil.DAY_MODE:
            default:
                setTheme(R.style.DayTheme);
                break;
        }
    }

    protected void configSystemBars(Window window) {
        Utils.transparentBars(window);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTheme();
        configSystemBars(getWindow());
        setContentView(onCreateRootView());
        ButterKnife.bind(this);
        startInject();
        parseExtras(getIntent());
        if (supportShareElement()) {
            postponeEnterTransition();
        }
        configToolBar(mToolbar);
        init();
        autoLoad();
    }

    protected boolean supportShareElement() {
        return false;
    }

    protected void autoLoad() {
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    protected ViewGroup onCreateRootView() {
        if ((mToolbar = attachToolbar()) != null) {
            LinearLayout rootView = new LinearLayout(this);
            rootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            rootView.setOrientation(LinearLayout.VERTICAL);
            rootView.addView(mToolbarWrapper == null ? mToolbar : mToolbarWrapper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            mContentView = rootView;
        }

        ViewGroup viewBelowToolbar;
        if (attachPtrHandler() != null) {
            V2erPtrFrameLayout ptrLayout = new V2erPtrFrameLayout(this);
            View content = getLayoutInflater().inflate(attachLayoutRes(), ptrLayout, false);
            ptrLayout.setContentView(content);
            ptrLayout.setPtrHandler(attachPtrHandler());
            viewBelowToolbar = ptrLayout;
        } else {
            viewBelowToolbar = (ViewGroup) getLayoutInflater().inflate(attachLayoutRes(), null);
        }

        if (mContentView != null) { //has toolbar
            mContentView.addView(viewBelowToolbar, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            mContentView = viewBelowToolbar;
            mContentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mRootView = new FrameLayout(this);
        mRootView.setId(R.id.act_root_view_framelayout);
        mRootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRootView.addView(mContentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRootView.setBackgroundColor(pageColor());
        return mRootView;
    }

    @ColorInt
    protected int pageColor() {
        return Theme.getColor(R.attr.page_bg_color, this);
    }

    @Nullable
    protected PtrFrameLayout getPtrLayout() {
        if (attachPtrHandler() == null) return null;
        PtrFrameLayout ptrLayout;
        if (mToolbar != null) {
            ptrLayout = (PtrFrameLayout) mContentView.getChildAt(1);
        } else {
            ptrLayout = (PtrFrameLayout) mContentView;
        }
        return ptrLayout;
    }

    protected AppCompatActivity getActivity() {
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }

    protected AppComponent getAppComponent() {
        return App.get().getAppComponent();
    }

    protected View onCreateLoadingView() {
        if (mLoadingView == null) {
            mLoadingView = LayoutInflater.from(this).inflate(R.layout.base_loading_view, mRootView, false);
            mRootView.addView(mLoadingView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            mLoadingView.bringToFront();
        }
        return mLoadingView;
    }

    @Override
    public void showLoading() {
        if (mLoadingView != null && mLoadingView.getVisibility() == View.VISIBLE) return;
        if (getPtrLayout() != null && getPtrLayout().isRefreshing()) return;
        onCreateLoadingView();
        if (mFirstLoadingDelay > 0) {
            mDelayLoadingRunnable = () -> {
                if (mFirstLoadingDelay != 0) {
                    mLoadingView.setVisibility(View.VISIBLE);
                    mFirstLoadingDelay = 0;
                    Logger.d("delay show loading");
                }
            };
            delay(mFirstLoadingDelay, mDelayLoadingRunnable);
        } else {
            mLoadingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoading() {
        if (getPtrLayout() != null) {
            getPtrLayout().refreshComplete();
        }
        if (mLoadingView != null) {
            if (mFirstLoadingDelay != 0) {
                //first loading doesn't show, yet
                if (mDelayLoadingRunnable != null) {
                    cancleRunnable(mDelayLoadingRunnable);
                    mFirstLoadingDelay = 0;
                    mDelayLoadingRunnable = null;
                }
            }
            mLoadingView.setVisibility(View.INVISIBLE);
        }
        Logger.d(" hide loading");
    }

    protected void toast(@StringRes int msgId) {
        toast(getString(msgId));
    }

    @Override
    public void toast(String msg) {
        Voast.show(msg);
    }

    public void toast(String msg, boolean isToastLong) {
        Voast.show(msg, isToastLong);
    }

    @Override
    public <K> LifecycleTransformer<K> bindToLife() {
        return bindToLifecycle();
    }


    @Override
    public <K> ObservableTransformer<K, K> rx() {
        return rx(this);
    }

    @Override
    public <K> ObservableTransformer<K, K> rx(int page) {
        return this.rx(page == 1 ? this : null);
    }

    @Override
    public <K> ObservableTransformer<K, K> rx(IViewLoading viewLoading) {
        return RxUtils.rxActivity(this, viewLoading);
    }

    @Override
    public void delay(long millisecond, Runnable runnable) {
        mContentView.postDelayed(runnable, millisecond);
    }

    protected void post(Runnable runnable) {
        delay(0, runnable);
    }

    protected void cancleRunnable(Runnable runnable) {
        mContentView.removeCallbacks(runnable);
    }

    protected static String KEY(String key) {
        return Utils.KEY(key);
    }

    @SuppressWarnings("unchecked")
    public <V extends View> V $(int id) {
        return (V) findViewById(id);
    }

    @Override
    public void handleError(GeneralError generalError) {
        if (supportShareElement()) {
            startPostponedEnterTransition();
        }
        if (generalError.getErrorCode() == ResultCode.LOGIN_EXPIRED || generalError.getErrorCode() == ResultCode.LOGIN_NEEDED) {
            handleNotLoginError(generalError.getErrorCode(), generalError.getMessage());
        } else if (generalError.getErrorCode() == ResultCode.REDIRECT_TO_HOME) {
            Navigator.from(this).setFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP).to(MainActivity.class).start();
            finish();
        } else if (generalError.getErrorCode() == ResultCode.LOGIN_TWO_STEP) {
            String once = APIService.fruit().fromHtml(generalError.getResponse(), TwoStepLoginInfo.class).getOnce();
            TwoStepLoginActivity.open(once, getActivity());
        } else {
            toast(generalError.getMessage());
        }
    }

    protected void handleNotLoginError(int errCode, String errorMsg) {
        toast(errorMsg);
        UserUtils.clearLogin();
        Navigator.from(getContext())
                .to(LoginActivity.class).start();
    }

    public void scheduleStartPostponedTransition(final View sharedElement) {
        if (sharedElement == null) return;
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        startPostponedEnterTransition();
                        return true;
                    }
                });
    }

    protected void finishToHome() {
        if (!MainActivity.isAlive) {
            Navigator.from(getActivity())
                    .addFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .to(MainActivity.class).start();
            super.finish();
            return;
        }
        super.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

}
