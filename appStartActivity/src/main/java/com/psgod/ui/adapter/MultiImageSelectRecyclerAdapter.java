package com.psgod.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.psgod.Constants;
import com.psgod.PsGodImageLoader;
import com.psgod.R;
import com.psgod.model.PhotoItem;
import com.psgod.model.SelectImage;
import com.psgod.ui.activity.PSGodBaseActivity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultiImageSelectRecyclerAdapter extends RecyclerView.Adapter<MultiImageSelectRecyclerAdapter.ViewHolde> {
    private final static String TAG = MultiImageSelectRecyclerAdapter.class
            .getSimpleName();

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_BANG = 1;

    private Context mContext;
    private List<SelectImage> mImages = new ArrayList<SelectImage>();
    private List<SelectImage> mSelectedImages = new ArrayList<SelectImage>();
    private List<PhotoItem> mPhotoItems = new ArrayList<PhotoItem>();

    private int checkedPhotoItem = 0;
    private int mItemSize;
    private int adapterType;

    public MultiImageSelectRecyclerAdapter(Context context, int adapterType) {
        mContext = context;
        this.adapterType = adapterType;
    }

    /**
     * 选择某个图片，改变选择状态
     *
     * @param image
     */
    public void select(SelectImage image) {
        if (mSelectedImages.contains(image)) {
            mSelectedImages.remove(image);
        } else {
            mSelectedImages.add(image);
        }
        notifyDataSetChanged();
    }

    /**
     * 通过图片路径设置默认选择
     *
     * @param resultList
     */
    public void setDefaultSelected(ArrayList<String> resultList) {
        for (String path : resultList) {
            SelectImage image = getImageByPath(path);
            if (image != null) {
                mSelectedImages.add(image);
            }
        }
        if (mSelectedImages.size() > 0) {
            notifyDataSetChanged();
        }
    }

    public SelectImage getImageByPath(String path) {
        if (mImages != null && mImages.size() > 0) {
            for (SelectImage image : mImages) {
                if (image.path.equalsIgnoreCase(path)) {
                    return image;
                }
            }
        }
        return null;
    }

    /**
     * 设置数据集
     *
     * @param images
     */
    public void setData(List<SelectImage> images) {
        mSelectedImages.clear();

        if (images != null && images.size() > 0) {
            mImages = images;
        } else {
            mImages.clear();
        }
        notifyDataSetChanged();
    }

    public void setBangData(List<PhotoItem> photoItems) {
        mPhotoItems = photoItems;
    }

    @Override
    public ViewHolde onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolde(LayoutInflater.from(mContext).
                inflate(R.layout.item_select_image_recycler, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolde holder, int position) {
        if (adapterType == TYPE_BANG) {
            holder.imageArea.setVisibility(View.INVISIBLE);
            holder.bangArea.setVisibility(View.VISIBLE);
            if (mPhotoItems != null) {
                PsGodImageLoader.getInstance().displayImage(mPhotoItems.get(position).getImageURL()
                        , holder.bangImage, Constants.DISPLAY_IMAGE_OPTIONS_SMALL_SMALL);
                PsGodImageLoader.getInstance().displayImage(mPhotoItems.get(position).getAvatarURL()
                        , holder.bangAvatar, Constants.DISPLAY_IMAGE_OPTIONS_AVATAR);
                holder.bangName.setText(mPhotoItems.get(position).getNickname());
                holder.bangDesc.setText(mPhotoItems.get(position).getDesc());
                if (position == checkedPhotoItem) {
                    holder.bangCheck.setImageResource(R.mipmap.zuopin_ic_sel_sel);
                } else {
                    holder.bangCheck.setImageResource(R.mipmap.zuopin_ic_sel_nor);
                }
                holder.itemView.setTag(R.id.tupppai_view_id,position);
                holder.itemView.setOnClickListener(bangClick);
            }
        } else {
            holder.imageArea.setVisibility(View.VISIBLE);
            holder.bangArea.setVisibility(View.INVISIBLE);
            holder.bindData(mImages.get(position));
            holder.itemView.setTag(R.id.tupppai_view_id, position);
            holder.itemView.setOnClickListener(imageClick);
        }
    }

    private View.OnClickListener imageClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Integer position = (Integer) view.getTag(R.id.tupppai_view_id);
            select(mImages.get(position));
        }
    };

    private View.OnClickListener bangClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Integer position = (Integer) view.getTag(R.id.tupppai_view_id);
            checkedPhotoItem = position;
            notifyDataSetChanged();
        }
    };

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return adapterType == TYPE_BANG ? mPhotoItems.size() : mImages.size();
    }

    class ViewHolde extends RecyclerView.ViewHolder {
        RelativeLayout imageArea;
        ImageView imageImage;
        ImageView imageCheck;

        RelativeLayout bangArea;
        ImageView bangAvatar;
        ImageView bangImage;
        TextView bangName;
        ImageView bangCheck;
        TextView bangDesc;

        ViewHolde(View view) {
            super(view);
            imageArea = (RelativeLayout) view.findViewById(R.id.item_select_image_imagearea);
            imageImage = (ImageView) view.findViewById(R.id.item_select_image_imagearea_image);
            imageCheck = (ImageView) view.findViewById(R.id.item_select_image_imagearea_checkmark);

            bangArea = (RelativeLayout) view.findViewById(R.id.item_select_image_bangarea);
            bangAvatar = (ImageView) view.findViewById(R.id.item_select_image_bangarea_avatar);
            bangImage = (ImageView) view.findViewById(R.id.item_select_image_bangarea_image);
            bangName = (TextView) view.findViewById(R.id.item_select_image_bangarea_name);
            bangCheck = (ImageView) view.findViewById(R.id.item_select_image_bangarea_checkmark);
            bangDesc = (TextView) view.findViewById(R.id.item_select_image_bangarea_desc);
        }

        void bindData(final SelectImage data) {
            if (data == null)
                return;
            // 处理单选和多选状态
            if (mSelectedImages.contains(data)) {
                // 设置选中状态
                bangCheck.setImageResource(R.mipmap.zuopin_ic_sel_sel);
                imageCheck.setImageResource(R.mipmap.zuopin_ic_sel_sel);
            } else {
                // 未选择
                bangCheck.setImageResource(R.mipmap.zuopin_ic_sel_nor);
                imageCheck.setImageResource(R.mipmap.zuopin_ic_sel_nor);
            }
            File imageFile = new File(data.path);
//			Bitmap bitmap = BitmapFactory.decodeFile()
            // 显示图片
            Picasso.with(mContext).load(imageFile)
                    .placeholder(R.drawable.default_error)
                            // .error(R.drawable.default_error)
                    .centerCrop().into(imageImage);
//                String uri = "file://" + imageFile.getPath();
//                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
//                ImageLoader.getInstance().displayImage(uri, image, Constants.DISPLAY_IMAGE_OPTIONS_LOCAL);
        }
    }

}