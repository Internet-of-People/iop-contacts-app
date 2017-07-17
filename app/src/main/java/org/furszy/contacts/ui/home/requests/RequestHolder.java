package org.furszy.contacts.ui.home.requests;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.furszy.contacts.R;
import org.furszy.contacts.adapter.BaseViewHolder;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by mati on 03/03/17.
 */
public class RequestHolder extends BaseViewHolder {

    TextView txt_name;
    Button btn_confirm;
    Button btn_cancel;

    public RequestHolder(View itemView, int holderType) {
        super(itemView,holderType);

        txt_name = (TextView) itemView.findViewById(R.id.txt_name);
        btn_confirm = (Button) itemView.findViewById(R.id.btn_confirm);
        btn_cancel = (Button) itemView.findViewById(R.id.btn_cancel);

    }
}