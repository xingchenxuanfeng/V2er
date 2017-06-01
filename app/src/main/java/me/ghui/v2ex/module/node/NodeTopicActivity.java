package me.ghui.v2ex.module.node;

import android.content.Intent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import javax.inject.Inject;

import butterknife.BindView;
import jp.wasabeef.glide.transformations.BlurTransformation;
import me.ghui.v2ex.R;
import me.ghui.v2ex.adapter.base.MultiItemTypeAdapter;
import me.ghui.v2ex.general.Navigator;
import me.ghui.v2ex.injector.component.DaggerNodeTopicComponnet;
import me.ghui.v2ex.injector.module.NodeTopicModule;
import me.ghui.v2ex.module.base.BaseActivity;
import me.ghui.v2ex.module.topic.TopicActivity;
import me.ghui.v2ex.network.bean.NodeInfo;
import me.ghui.v2ex.network.bean.NodesInfo;
import me.ghui.v2ex.widget.LoadMoreRecyclerView;
import me.ghui.v2ex.widget.listener.AppBarStateChangeListener;

/**
 * Created by ghui on 25/05/2017.
 * 节点话题页
 */

// TODO: 25/05/2017
public class NodeTopicActivity extends BaseActivity<NodeTopicContract.IPresenter> implements NodeTopicContract.IView,
        MultiItemTypeAdapter.OnItemClickListener, LoadMoreRecyclerView.OnLoadMoreListener {
    public static final String TAG_LINK_KEY = KEY("tag_link_key");
    private String mTagId;
    private String mTagLink;

    @BindView(R.id.common_recyclerview)
    LoadMoreRecyclerView mRecyclerView;
    @BindView(R.id.node_img)
    ImageView mNodeImg;
    @BindView(R.id.big_img_bg)
    ImageView mBigImgBg;
    @BindView(R.id.node_name_tv)
    TextView mNodeText;
    @BindView(R.id.node_describtion_tv)
    TextView mNodeDesTv;
    @BindView(R.id.collapsing_toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbarLayout;
    @BindView(R.id.node_info_appbar_layout)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.node_info_toobar)
    Toolbar mToolbar;
    @BindView(R.id.node_topic_num)
    TextView mNodeTopicNumTv;
    @BindView(R.id.node_star_num)
    TextView mNodeStarNumTv;
    @BindView(R.id.node_info_star_ct)
    CheckedTextView mStarBtn;

    @Inject
    LoadMoreRecyclerView.Adapter<NodesInfo.Item> mAdapter;
    private NodeInfo mNodeInfo;

    private MenuItem mLoveMenuItem;

    @Override
    protected int attachLayoutRes() {
        return R.layout.act_tag_page;
    }

    @Override
    protected Toolbar attachToolbar() {
        return null;
    }

    @Override
    protected void parseExtras(Intent intent) {
        mTagLink = intent.getStringExtra(TAG_LINK_KEY);
        mTagId = mTagLink.substring(mTagLink.lastIndexOf("/") + 1);
    }

    @Override
    protected void startInject() {
        DaggerNodeTopicComponnet.builder()
                .appComponent(getAppComponent())
                .nodeTopicModule(new NodeTopicModule(this))
                .build().inject(this);
    }

    @Override
    protected void init() {
        mToolbar.inflateMenu(R.menu.note_info_toolbar_menu);
        mLoveMenuItem = mToolbar.getMenu().findItem(R.id.action_star);
        mToolbar.setNavigationOnClickListener(view -> onBackPressed());
        mToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_star) {
                Toast.makeText(getContext(), "do Star...", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        mRecyclerView.setOnLoadMoreListener(this);
        mRecyclerView.addDivider();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(this);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, State state) {
                if (state == State.EXPANDED) {
                    //展开状态
                    mToolbar.setTitle(null);
                    mToolbar.setSubtitle(null);
                    mLoveMenuItem.setVisible(false);
                } else if (state == State.COLLAPSED) {
                    //折叠状态
                    mLoveMenuItem.setVisible(true);
                    if (mNodeInfo != null) {
                        mToolbar.setTitle(mNodeInfo.getTitle());
                        mToolbar.setSubtitle(mNodeInfo.getTopics() + " 个主题");
                    }
                } else {
                    //中间状态
                }
            }
        });
    }

    @Override
    protected void autoLoad() {
        mPresenter.start();
    }

    @Override
    public String nodeName() {
        return mTagId;
    }

    @Override
    public void fillHeaderView(NodeInfo nodeInfo) {
        if (nodeInfo == null) return;
        mNodeInfo = nodeInfo;
        mCollapsingToolbarLayout.setTitle(nodeInfo.getTitle());
        mNodeText.setText(nodeInfo.getTitle());
        mNodeDesTv.setText(nodeInfo.getHeader());
        mNodeTopicNumTv.setText(mNodeInfo.getTopics() + " 个主题");
        mNodeStarNumTv.setText(mNodeInfo.getStars() + " 个收藏");
        Glide.with(this)
                .load(nodeInfo.getAvatar())
                .into(mNodeImg);
        Glide.with(this).load(nodeInfo.getAvatar())
                .bitmapTransform(new BlurTransformation(this))
                .into(mBigImgBg);
    }

    @Override
    public void fillListView(NodesInfo nodesInfo, boolean isLoadMore) {
        if (nodesInfo == null) {
            mAdapter.setData(null);
            return;
        }
        mAdapter.setData(nodesInfo.getItems(), isLoadMore);
        mRecyclerView.setHasMore(nodesInfo.getTotal() > mAdapter.getContentItemCount());
        toggleStar(nodesInfo.hasStared());
    }

    private void toggleStar(boolean isStared) {
        mLoveMenuItem.setIcon(isStared ?
                R.drawable.love_checked_icon : R.drawable.love_normal_icon);
        mStarBtn.setChecked(isStared);
        mStarBtn.setText(isStared ? "已收藏" : "收藏");
    }

    @Override
    public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
        String link = mAdapter.getDatas().get(position).getTopicLink();
        Navigator.from(getContext())
                .to(TopicActivity.class)
                .putExtra(TopicActivity.TOPIC_LINK_KEY, link)
                .start();
    }

    @Override
    public void onLoadMore(int willLoadPage) {
        mPresenter.loadData(willLoadPage);
    }
}
