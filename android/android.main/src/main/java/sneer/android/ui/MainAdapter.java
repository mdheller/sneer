package sneer.android.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import sneer.Conversation;
import sneer.android.R;

import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.ui.SneerActivity.*;

public class MainAdapter extends ArrayAdapter<Conversation> {

	private final Activity activity;
	private final CompositeSubscription subscriptions;

	public MainAdapter(Activity activity) {
        super(activity, R.layout.list_item_main);
        this.activity = activity;
		subscriptions = new CompositeSubscription();
    }


    @Override
    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(R.layout.list_item_main, parent, false);

			ConversationHolder holder = new ConversationHolder();
            holder.conversationParty = findView(row, R.id.conversationParty);
            holder.conversationSummary = findView(row, R.id.conversationSummary);
            holder.conversationDate = findView(row, R.id.conversationDate);
            holder.conversationPicture = findView(row, R.id.conversationPicture);
            holder.conversationUnread = findView(row, R.id.conversationUnread);

            Shader textShader = new LinearGradient(200, 0, 650, 0,
            		new int[] {Color.DKGRAY, Color.LTGRAY},
            		new float[] {0, 1}, TileMode.CLAMP);
            holder.conversationSummary.getPaint().setShader(textShader);

            row.setTag(holder);

			subscriptions.add(subscribeToConversationAt(position, holder));
        }

        return row;
    }

	private Subscription subscribeToConversationAt(int position, ConversationHolder holder) {
		Conversation conversation = getItem(position);
		return Subscriptions.from(
				plug(holder.conversationParty, conversation.party().name()),
				plug(holder.conversationSummary, conversation.mostRecentMessageContent()),
				plug(holder.conversationPicture, sneer().profileFor(conversation.party()).selfie()),
				plugUnreadMessage(holder.conversationUnread, conversation.unreadMessageCount()),
				plugDate(holder.conversationDate, conversation.mostRecentMessageTimestamp())
		);
	}


	static class ConversationHolder {
		TextView conversationParty;
		TextView conversationSummary;
		TextView conversationDate;
		TextView conversationUnread;
		ImageView conversationPicture;
	}


	public static Subscription plugUnreadMessage(final TextView textView, Observable<Long> observable) {
		return deferUI(observable).subscribe(new Action1<Long>() { @Override public void call(Long obj) {
			if (obj == 0)
				textView.setVisibility(View.GONE);
			else
				textView.setVisibility(View.VISIBLE);
			textView.setText(obj.toString());
		}});
	}


	public void dispose() {
		subscriptions.unsubscribe();
	}

}
