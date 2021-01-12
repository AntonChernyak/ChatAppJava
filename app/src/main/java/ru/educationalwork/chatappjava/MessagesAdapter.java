package ru.educationalwork.chatappjava;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder> {

    private List<Message> messageList;

    public MessagesAdapter() {
        messageList = new ArrayList<>();
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_message, parent, false);
        return new MessagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesViewHolder holder, int position) {
        Message message = messageList.get(position);
        String author = message.getAuthor();
        String textOfMessage = message.getTextOfMessage();
        String urlToMessage = message.getImageUrl();

        holder.textViewAuthor.setText(author);
        if (textOfMessage != null && !textOfMessage.isEmpty()) {
            holder.textViewMessage.setText(textOfMessage);
            holder.imageViewImage.setVisibility(View.GONE);
        }

        if (urlToMessage != null && !urlToMessage.isEmpty()){
            holder.imageViewImage.setVisibility(View.VISIBLE);
            Picasso.get().load(urlToMessage).into(holder.imageViewImage);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }


    class MessagesViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewAuthor;
        private TextView textViewMessage;
        private ImageView imageViewImage;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            imageViewImage = itemView.findViewById(R.id.imadeViewImage);
        }
    }
}
