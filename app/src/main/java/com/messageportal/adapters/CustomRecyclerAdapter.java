package com.messageportal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.messageportal.R;
import com.messageportal.entities.SMSEntity;
import com.messageportal.utils.Constants;
import com.messageportal.utils.Utils;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;
import net.steamcrafted.materialiconlib.MaterialIconView;
import java.util.List;

public class CustomRecyclerAdapter extends RecyclerView.Adapter<CustomRecyclerAdapter.MyViewHolder> {

    private List<SMSEntity> smsEntities;
    private Context appContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView receiver, smsBody, sentDate;
        public MaterialIconView smsImage;

        public MyViewHolder(View view) {
            super(view);
            receiver = (TextView) view.findViewById(R.id.receiver);
            smsBody = (TextView) view.findViewById(R.id.sms_body);
            sentDate = (TextView) view.findViewById(R.id.sms_date);
            smsImage = (MaterialIconView)view.findViewById(R.id.sms_img);
        }
    }

    public CustomRecyclerAdapter(List<SMSEntity> sentSMS, Context context) {
        this.smsEntities = sentSMS;
        this.appContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        SMSEntity entityDetail = smsEntities.get(position);
        holder.sentDate.setText(Utils.getStringFromDate(entityDetail.getSentOn()));
        holder.receiver.setText(entityDetail.getReceiver());
        holder.smsBody.setText(entityDetail.getBody());
        holder.smsImage.setIcon(MaterialDrawableBuilder.IconValue.CELLPHONE_ANDROID);
        if(entityDetail.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.SENT.getName()))
            holder.smsImage.setColorResource(R.color.app_primary);
        else if(entityDetail.getStatus().equalsIgnoreCase(Constants.SMS_STATUS.ERROR.getName()))
            holder.smsImage.setColorResource(R.color.red);
    }

    @Override
    public int getItemCount() {
        if(smsEntities != null) {
            return smsEntities.size();
        }
        return 0;
    }

    public void refresh(List<SMSEntity> smsEntityList)
    {
        this.smsEntities = smsEntityList;
        notifyDataSetChanged();
    }
}
