package me.ghui.v2er.module.append;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.EditText;

import butterknife.BindView;
import me.ghui.toolbox.android.Check;
import me.ghui.v2er.R;
import me.ghui.v2er.general.ActivityReloader;
import me.ghui.v2er.general.Navigator;
import me.ghui.v2er.injector.component.DaggerAppendTopicComponnet;
import me.ghui.v2er.injector.module.AppendTopicModule;
import me.ghui.v2er.module.base.BaseActivity;
import me.ghui.v2er.module.topic.TopicActivity;
import me.ghui.v2er.network.bean.AppendTopicPageInfo;
import me.ghui.v2er.network.bean.TopicInfo;
import me.ghui.v2er.util.Utils;
import me.ghui.v2er.util.Voast;
import me.ghui.v2er.widget.BaseToolBar;
import me.ghui.v2er.widget.dialog.ConfirmDialog;

public class AppendTopicActivity extends BaseActivity<AppendTopicContract.IPresenter> implements AppendTopicContract.IView {
    public static final String KEY_TOPIC_ID = KEY("topic_id_key");
    private static final String KEY_PAGE_INFO = KEY("page_info");
    private static final String KEY_CONTENT = KEY("append_content");
    private String mTopicId;
    private AppendTopicPageInfo mPageInfo;

    @BindView(R.id.append_topic_content_et)
    EditText mContentET;


    public static void open(String topicId, Context context) {
        Navigator.from(context)
                .to(AppendTopicActivity.class)
                .putExtra(KEY_TOPIC_ID, topicId)
                .start();
    }

    @Override
    protected void parseExtras(Intent intent) {
        mTopicId = intent.getStringExtra(KEY_TOPIC_ID);
        mPresenter.setTopicId(mTopicId);
    }

    @Override
    protected int attachLayoutRes() {
        return R.layout.act_append_topic;
    }

    @Override
    protected void startInject() {
        DaggerAppendTopicComponnet.builder().appComponent(getAppComponent())
                .appendTopicModule(new AppendTopicModule(this))
                .build().inject(this);
    }

    @Override
    protected void configToolBar(BaseToolBar toolBar) {
        super.configToolBar(toolBar);
        //设置右上角的填充菜单
        toolBar.inflateMenu(R.menu.append_topic_menu);
        Utils.setPaddingForStatusBar(toolBar);
        Utils.setPaddingForNavbar(mRootView);
        toolBar.setOnMenuItemClickListener(item -> {
            String content = mContentET.getText().toString();
            if (Check.isEmpty(content)) {
                Voast.show("请输入附言内容");
                return false;
            }
            mPresenter.sendAppend(content);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (!TextUtils.isEmpty(mContentET.getText().toString())) {
            new ConfirmDialog.Builder(this)
                    .title("丢弃附言")
                    .msg("返回将丢弃当前编写的内容")
                    .positiveText(R.string.ok, dialog -> finish())
                    .negativeText(R.string.cancel)
                    .build().show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void init() {
        Intent intent = getIntent();
        mContentET.setText(intent.getStringExtra(KEY_CONTENT));
        mPageInfo = (AppendTopicPageInfo) intent.getSerializableExtra(KEY_PAGE_INFO);
        mTopicId = intent.getStringExtra(KEY_TOPIC_ID);
    }

    @Override
    protected void autoLoad() {
        if (mPageInfo == null) {
            super.autoLoad();
        }
    }

    @Override
    protected void reloadMode(int mode) {
        ActivityReloader.target(this)
                .putExtra(KEY_TOPIC_ID, mTopicId)
                .putExtra(KEY_PAGE_INFO, mPageInfo)
                .putExtra(KEY_CONTENT, mContentET.getText().toString())
                .reload();
    }

    @Override
    public void fillView(AppendTopicPageInfo pageInfo) {
        mPageInfo = pageInfo;
        StringBuilder hint = new StringBuilder("在此输入附言内容...\n\n");
        for (int i = 0; i < mPageInfo.getTips().size(); i++) {
            hint.append(i + 1 + ": " + mPageInfo.getTips().get(i).text + "\n");
        }
        mContentET.setHint(hint.toString());
    }

    @Override
    public void onPostSuccess(TopicInfo topicInfo) {
        Utils.toggleKeyboard(false, mContentET);
        Navigator.from(this)
                .to(TopicActivity.class)
                .addFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(TopicActivity.TOPIC_INTO_KEY, topicInfo)
                .putExtra(TopicActivity.TOPIC_ID_KEY, mTopicId)
                .start();
        // TODO: 2020-01-10 你不能为一个创建30分钟内的主题添加附言
    }

    @Override
    public void onPostFailure() {

    }

}
