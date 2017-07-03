package me.ghui.v2er.module.topic;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import javax.annotation.Nullable;

import me.ghui.v2er.R;
import me.ghui.v2er.adapter.base.ItemViewDelegate;
import me.ghui.v2er.adapter.base.ViewHolder;
import me.ghui.v2er.general.PreConditions;
import me.ghui.v2er.network.bean.TopicInfo;

/**
 * Created by ghui on 09/05/2017.
 */

public class TopicReplyItemDelegate extends ItemViewDelegate<TopicInfo.Item> {

    public TopicReplyItemDelegate(Context context) {
        super(context);
    }

    @Override
    public int getItemViewLayoutId() {
        return R.layout.topic_reply_item;
    }

    @Override
    public boolean isForViewType(@Nullable TopicInfo.Item item, int position) {
        return item != null && !item.isHeaderItem();
    }

    @Override
    public void convert(ViewHolder holder, TopicInfo.Item item, int position) {
        TopicInfo.Reply replyInfo = (TopicInfo.Reply) item;
        Glide.with(mContext)
                .load(replyInfo.getAvatar())
                .into((ImageView) holder.getView(R.id.reply_avatar_img));
        // TODO: 01/07/2017 楼主标记 
        holder.setText(R.id.reply_user_name_tv, replyInfo.getUserName());
        if (replyInfo.getLove() == 0) {
            holder.getView(R.id.reply_thx_tv).setVisibility(View.GONE);
        } else {
            holder.getView(R.id.reply_thx_tv).setVisibility(View.VISIBLE);
            holder.setText(R.id.reply_thx_tv, replyInfo.getLove() + "");
        }
        ImageView img = holder.getView(R.id.reply_thx_img);
        if (!item.isSelf()) {
            img.setVisibility(View.VISIBLE);
            img.setImageResource(replyInfo.hadThanked() ? R.drawable.love_checked_icon : R.drawable.love_normal_icon);
        } else {
            img.setVisibility(View.GONE);
        }
        holder.setText(R.id.time_tv, replyInfo.getTime());
        HtmlTextView contentView = (HtmlTextView) holder.getView(R.id.content_tv);
        if (PreConditions.notEmpty(replyInfo.getReplyContent())) {
            contentView.setVisibility(View.VISIBLE);
//            RichText.fromHtml(replyInfo.getReplyContent()).clickable(false).into(contentView);
//            contentView.setText(Html.fromHtml(replyInfo.getReplyContent()));
            contentView = holder.getView(R.id.content_tv);
            contentView.setHtml(replyInfo.getReplyContent(), new HtmlHttpImageGetter(contentView));
        } else {
            contentView.setVisibility(View.GONE);
        }
        holder.setText(R.id.floor_tv, replyInfo.getFloor());
    }
}
