package me.ghui.v2er.module.login;


import android.util.Log;

import me.ghui.v2er.BuildConfig;
import me.ghui.v2er.network.bean.ErrorRequestInfo;
import me.ghui.v2er.util.Check;
import me.ghui.v2er.R;
import me.ghui.v2er.general.App;
import me.ghui.v2er.network.APIService;
import me.ghui.v2er.network.GeneralConsumer;
import me.ghui.v2er.network.bean.BaseInfo;
import me.ghui.v2er.network.bean.DailyInfo;
import me.ghui.v2er.network.bean.LoginParam;
import me.ghui.v2er.network.bean.TwoStepLoginInfo;
import me.ghui.v2er.network.bean.UserInfo;
import me.ghui.v2er.util.UserUtils;

/**
 * Created by ghui on 27/03/2017.
 */

public class LoginPresenter implements LoginContract.IPresenter {

    private LoginContract.IView mView;
    private LoginParam mLoginParam;
    private LoginParam mFailCacheLoginParam;

    public LoginPresenter(LoginContract.IView mView) {
        this.mView = mView;
    }

    @Override
    public void start() {
        if (mFailCacheLoginParam != null && mFailCacheLoginParam.isValid()) {
            mView.onFetchLoginParamSuccess(mFailCacheLoginParam);
            mLoginParam = mFailCacheLoginParam;
            mFailCacheLoginParam = null;
            return;
        }
        APIService.get().loginParam()
                .compose(mView.rx(null))
                .subscribe(new GeneralConsumer<LoginParam>(mView) {
                    @Override
                    public void onConsume(LoginParam loginParam) {
                        if (loginParam.isValid()) {
                            mLoginParam = loginParam;
                            mView.onFetchLoginParamSuccess(loginParam);
                        } else {
                            mView.onFetchLoginParamFailure();
                        }
                    }
                });
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void login(String userName, String psw, String captcha) {
        APIService.get().login(mLoginParam.toMap(userName, psw, captcha))
                .compose(mView.rx())
                .map(response -> response.body().string())
                .map(s -> {
                    Log.e("testtest", s);
                    DailyInfo resultInfo = APIService.fruit().fromHtml(s, DailyInfo.class);
                    if (!resultInfo.isValid()) {//check whether is login success
                        LoginParam loginParam = APIService.fruit().fromHtml(s, LoginParam.class);
                        if (!loginParam.isValid()) {//check whether is psw incorrect
                            //you may enabled two step login
                            TwoStepLoginInfo twoStepLoginInfo = APIService.fruit().fromHtml(s, TwoStepLoginInfo.class);
                            if (twoStepLoginInfo != null) {
                                return twoStepLoginInfo;
                            } else {
                                return new ErrorRequestInfo(s);
                            }
                        }
                        return loginParam;
                    }
                    return resultInfo;
                })
                .subscribe(new GeneralConsumer<BaseInfo>(mView) {
                    @Override
                    public void onError(Throwable e) {
//                        super.onError(e);
                        Log.e("LoginPresenter", "onError", e);
                    }

                    @Override
                    public void onConsume(BaseInfo info) {
                        if (info instanceof DailyInfo) {
                            // login success
                            DailyInfo resultInfo = (DailyInfo) info;
                            UserUtils.saveLogin(UserInfo.build(resultInfo.getUserName(), resultInfo.getAvatar()));
                            mView.onLoginSuccess();
                        } else if (info instanceof LoginParam) {
                            //login failure
                            LoginParam loginParam = (LoginParam) info;
                            String problemHtml = loginParam.getProblem();
                            if (loginParam.isValid() && Check.isEmpty(problemHtml)) {
                                mLoginParam = loginParam;
                                mView.onLoginFailure("登录失败，用户名和密码无法匹配", false);
                            } else if (Check.notEmpty(problemHtml)) {
                                mLoginParam = loginParam;
                                mView.onLoginFailure(problemHtml, true);
                            } else {
                                mView.onLoginFailure(App.get().getString(R.string.login_occur_unknown_error), false);
                            }
                            mFailCacheLoginParam = loginParam;
                        } else if (info instanceof TwoStepLoginInfo) {
                            //you may enabled two step login
                            TwoStepLoginInfo twoStepLoginInfo = (TwoStepLoginInfo) info;
                            TwoStepLoginActivity.open(twoStepLoginInfo.getOnce(), mView.getContext());
                            mView.onOccuredTwoStep();
                        } else if (info instanceof ErrorRequestInfo) {
                            if (BuildConfig.DEBUG) {
                                mView.onLoginFailure(((ErrorRequestInfo) info).getMsg(), true);
                            } else {
                                mView.onLoginFailure(App.get().getString(R.string.login_occur_unknown_error), false);
                            }
                        }
                    }
                });
    }

    @Override
    public void signInWithGoogle() {
        SignInWithGoogleActivity.open(mView.getContext(), mLoginParam.getOnce());
    }

}
