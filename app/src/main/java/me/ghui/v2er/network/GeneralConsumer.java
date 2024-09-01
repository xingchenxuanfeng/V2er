package me.ghui.v2er.network;

import android.util.Log;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import me.ghui.v2er.BuildConfig;
import me.ghui.v2er.network.bean.BaseInfo;
import me.ghui.v2er.network.bean.IBase;
import me.ghui.v2er.network.bean.LoginParam;
import me.ghui.v2er.network.bean.NewsInfo;
import me.ghui.v2er.network.bean.TwoStepLoginInfo;
import me.ghui.v2er.util.L;
import me.ghui.v2er.util.RxUtils;
import me.ghui.v2er.util.UserUtils;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.util.Voast;
import retrofit2.HttpException;

/**
 * Created by ghui on 19/06/2017.
 */

public abstract class GeneralConsumer<T extends IBase> implements Observer<T> {

    private IGeneralErrorHandler mGeneralErrorHandler;

    private RuntimeException preExceptionOnSubscribe;

    public GeneralConsumer(IGeneralErrorHandler generalErrorHandler) {
        mGeneralErrorHandler = generalErrorHandler;
    }

    public GeneralConsumer() {
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (BuildConfig.DEBUG) {
            preExceptionOnSubscribe = new RuntimeException();
        }
    }

    @Override
    public void onNext(T t) {
        if (t == null) {
            L.e("API RESPONSE: NULL");
            onError(new Throwable("Unknown Error"));
            return;
        }
        L.v("API RESPONSE: \n" + t.toString() + "\n");
        if (t.isValid()) {
            onConsume(t);
        } else {
            /*
             Possible Reasons:
                1. need login but no login
                2. need login but login is expired
                3. no premission to open the page
            */
            GeneralError generalError = new GeneralError(ResultCode.NETWORK_ERROR, "Unknown Error");
            String response = t.getResponse();
            generalError.setResponse(response);
            Observable.just(response)
                    .compose(RxUtils.io_main())
                    .map(s -> {
                        BaseInfo resultInfo = APIService.fruit().fromHtml(s, LoginParam.class);
                        if (resultInfo == null) return null;
                        if (!resultInfo.isValid()) {
                            resultInfo = APIService.fruit().fromHtml(s, NewsInfo.class);
                        }
                        if (!resultInfo.isValid()) {
                            resultInfo = APIService.fruit().fromHtml(s, TwoStepLoginInfo.class);
                        }
                        // 31/07/2017 More tries...
                        return resultInfo;
                    })
                    .subscribe(new BaseConsumer<BaseInfo>() {
                        @Override
                        public void onConsume(BaseInfo resultInfo) {
                            if (resultInfo == null || !resultInfo.isValid()) {
                                onGeneralError(generalError);
                                return;
                            }
                            if (resultInfo instanceof LoginParam) {
                                if (UserUtils.isLogin()) {
                                    generalError.setErrorCode(ResultCode.LOGIN_EXPIRED);
                                    generalError.setMessage("登录已过期，请重新登录");
                                    UserUtils.clearLogin();
                                } else {
                                    generalError.setErrorCode(ResultCode.LOGIN_NEEDED);
                                    generalError.setMessage("需要您先去登录");
                                }
                            } else if (resultInfo instanceof NewsInfo) {
                                generalError.setErrorCode(ResultCode.REDIRECT_TO_HOME);
                                generalError.setMessage("Redirecting to home");
                            } else if (resultInfo instanceof TwoStepLoginInfo) {
                                generalError.setErrorCode(ResultCode.LOGIN_TWO_STEP);
                                generalError.setMessage("Two Step Login");
                            }
                            onGeneralError(generalError);
                        }

                        @Override
                        public void onError(Throwable e) {
                            onGeneralError(e);
                        }
                    });
        }
    }

    public abstract void onConsume(T t);

    private void onGeneralError(Throwable e) {
        this.onError(e);
    }

    @Override
    public void onError(Throwable e) {
        if (BuildConfig.DEBUG && preExceptionOnSubscribe != null) {
            preExceptionOnSubscribe.addSuppressed(e);
            Log.e("GeneralConsumer", "onError", preExceptionOnSubscribe);
//            throw preExceptionOnSubscribe;
            Voast.show(e.getMessage());
        }
        GeneralError generalError;
        if (e instanceof GeneralError) {
            generalError = (GeneralError) e;
        } else {
            if (e instanceof HttpException) {
                HttpException he = (HttpException) e;
                generalError = new GeneralError(he.code(), he.message());
            } else {
                // 未知错误
                String msg = "Unknown Error";
                if (!Utils.isNetworkAvailable()) msg = "Network Connection Error";
                generalError = new GeneralError(ResultCode.NETWORK_ERROR, msg);
                if (BuildConfig.DEBUG) {
//                    throw new RuntimeException(e);
                }
            }
        }

        if (mGeneralErrorHandler == null) {
            Voast.show(generalError.toast());
        } else {
            mGeneralErrorHandler.handleError(generalError);
        }
    }

    @Override
    public void onComplete() {

    }
}
