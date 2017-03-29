package me.ghui.v2ex.module.base;


import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trello.rxlifecycle2.LifecycleTransformer;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by ghui on 05/03/2017.
 */

public abstract class BaseFragment<T extends BaseContract.IBasePresenter> extends RxFragment implements BaseContract.IBaseView {

	//root view of this fragment
	protected ViewGroup mBaseRootView;
	@Inject
	protected T mPresenter;


	/**
	 * bind a layout resID to the content of this page
	 *
	 * @return
	 */
	@LayoutRes
	protected abstract int attachLayoutRes();

	/**
	 * init Dagger2 injector
	 */
	protected abstract void initInjector();

	/**
	 * init views in this page
	 */
	protected abstract void init();

	/**
	 * fetch data from server to views in this page
	 */
	protected abstract void fetchData();


	@Nullable
	@Override
	@CallSuper
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (mBaseRootView == null) {
			mBaseRootView = (ViewGroup) inflater.inflate(attachLayoutRes(), null);
			ButterKnife.bind(this, mBaseRootView);
			initInjector();
			init();
		}
		ViewGroup parent = (ViewGroup) mBaseRootView.getParent();
		if (parent != null) {
			parent.removeView(mBaseRootView);
		}
		return mBaseRootView;
	}

	@Override
	public void showLoading() {

	}

	@Override
	public void hideLoading() {

	}

	@Override
	public <T> LifecycleTransformer<T> bindToLife() {
		return bindToLifecycle();
	}
}
