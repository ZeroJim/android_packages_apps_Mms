/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.util.EmojiParser;
import com.android.mms.util.SmileyParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given conversation.
 */
public class ConversationListItem extends RelativeLayout implements Contact.UpdateListener,
            Checkable {
    private static final String TAG = "ConversationListItem";
    private static final boolean DEBUG = false;
	private TextView mUnReadNum;
    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private View mAttachmentView;
    private View mErrorIndicator;
    private Context mContext;
    private QuickContactBadge mAvatarView;

    //shutao 2013-2-22
    private CheckBox mAvatarCheckBox;
    static private Drawable sDefaultContactImage;

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();

    private Conversation mConversation;

    public static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    public ConversationListItem(Context context) {
        super(context);
    	mContext = context;
    }

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    	mContext = context;
        if (sDefaultContactImage == null) {
            /*Wang: 2012-11-15*/
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture_1);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.from);
        mSubjectView = (TextView) findViewById(R.id.subject);
    	// add by shendu liuchuan
		mUnReadNum = (TextView) findViewById(R.id.avatarnum);
        mDateView = (TextView) findViewById(R.id.date);
        mAttachmentView = findViewById(R.id.attachment);
        mErrorIndicator = findViewById(R.id.error);
        mAvatarView = (QuickContactBadge) findViewById(R.id.avatar);
        //shutao 2013-2-22
        mAvatarCheckBox = (CheckBox) findViewById(R.id.avatar_box);
    }

    public Conversation getConversation() {
        return mConversation;
    }

    /**
     * Only used for header binding.
     */
    public void bind(String title, String explain) {
        mFromView.setText(title);
        mSubjectView.setText(explain);
    }

    private CharSequence formatMessage() {
//        final int color = android.R.styleable.Theme_textColorSecondary;
    	 final int color = R.color.shendu_conversation_text_bule;
        String from = mConversation.getRecipients().formatNames(", ");

        SpannableStringBuilder buf = new SpannableStringBuilder(from);
//shutao 2013-2-19
//        if (mConversation.getMessageCount() > 1) {
//            int before = buf.length();
//            buf.append(mContext.getResources().getString(R.string.message_count_format,
//                    "("+mConversation.getMessageCount()+")"));
//            buf.setSpan(new ForegroundColorSpan(
//                    mContext.getResources().getColor(R.color.message_count_color)),
//                    before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//        }
        try{
        if (mConversation.hasDraft()) {
            buf.append(mContext.getResources().getString(R.string.draft_separator));
            int before = buf.length();
            int size;
            buf.append(mContext.getResources().getString(R.string.has_draft));
//            size = android.R.style.TextAppearance_Small;
//            buf.setSpan(new TextAppearanceSpan(mContext, size, color), before,
//                    buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.setSpan(new ForegroundColorSpan(
                    mContext.getResources().getColor(R.color.shendu_conversation_text_bule)),
                    before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        // Unread messages are shown in bold
        if (mConversation.hasUnreadMessages()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.setSpan(new ForegroundColorSpan(
                    mContext.getResources().getColor(R.color.shendu_conversation_text_bule)),
                    0, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        }catch(Exception e){
        	e.printStackTrace();
        }
        return buf;
    }

    private void updateAvatarView() {
        Drawable avatarDrawable;
        if (mConversation.getRecipients().size() == 1) {
            Contact contact = mConversation.getRecipients().get(0);
            avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);

            if (contact.existsInDatabase()) {
                mAvatarView.assignContactUri(contact.getUri());
            } else {
                mAvatarView.assignContactFromPhone(contact.getNumber(), true);
            }
        } else {
            // TODO get a multiple recipients asset (or do something else)
//            avatarDrawable = sDefaultContactImage;
        	 avatarDrawable = mContext.getResources().getDrawable(R.drawable.ic_contact_group);
            mAvatarView.assignContactUri(null);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }

    private void updateFromView() {
        mFromView.setText(formatMessage());
        updateAvatarView();
    }

    public void onUpdate(Contact updated) {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "onUpdate: " + this + " contact: " + updated);
        }
        mHandler.post(new Runnable() {
            public void run() {
                updateFromView();
            }
        });
    }

    public final void bind(Context context, final Conversation conversation) {
        //if (DEBUG) Log.v(TAG, "bind()");

        mConversation = conversation;

        updateBackground();
     // add by shendu liuchuan
     		if (conversation.hasUnreadMessages()) {
     			mUnReadNum.setText(getUnReadUnm(conversation.getThreadId())
     					.getCount() + "");
     			mUnReadNum.setVisibility(View.VISIBLE);
     		} else {
     			mUnReadNum.setVisibility(View.GONE);
     		}
        LayoutParams attachmentLayout = (LayoutParams)mAttachmentView.getLayoutParams();
        boolean hasError = conversation.hasError();
        // When there's an error icon, the attachment icon is left of the error icon.
        // When there is not an error icon, the attachment icon is left of the date text.
        // As far as I know, there's no way to specify that relationship in xml.
        if (hasError) {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.error);
        } else {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.date);
        }

        boolean hasAttachment = conversation.hasAttachment();
        mAttachmentView.setVisibility(hasAttachment ? VISIBLE : GONE);

        // Date
        mDateView.setText(MessageUtils.formatTimeStampString(context, conversation.getDate()));

        // From. shutao 2013-2-19
        mFromView.setText(formatMessage());

        // Register for updates in changes of any of the contacts in this conversation.
        ContactList contacts = conversation.getRecipients();

        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "bind: contacts.addListeners " + this);
        }
        Contact.addListener(this);

        // Subject
        SmileyParser parser = SmileyParser.getInstance();
        CharSequence smileySubject = parser.addSmileySpans(conversation.getSnippet());
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        boolean enableEmojis = prefs.getBoolean(MessagingPreferenceActivity.ENABLE_EMOJIS, false);
        if(enableEmojis) {
            EmojiParser emojiParser = EmojiParser.getInstance();
            smileySubject = emojiParser.addEmojiSpans(smileySubject);
        }
        mSubjectView.setText(smileySubject);
        LayoutParams subjectLayout = (LayoutParams)mSubjectView.getLayoutParams();
        // We have to make the subject left of whatever optional items are shown on the right.
        subjectLayout.addRule(RelativeLayout.LEFT_OF, hasAttachment ? R.id.attachment :
            (hasError ? R.id.error : R.id.date));

        // Transmission error indicator.
        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);

        updateAvatarView();
    }

    private void updateBackground() {
        int backgroundId;
        if (mConversation.isChecked()) {
            backgroundId = R.drawable.list_selected_holo_light;
        } else if (mConversation.hasUnreadMessages()) {
            backgroundId = R.drawable.conversation_item_background_unread;
        } else {
            backgroundId = R.drawable.conversation_item_background_read;
        }
        Drawable background = mContext.getResources().getDrawable(backgroundId);
        setBackground(background);
    }

    //add by shendu liuchuan
	private Cursor getUnReadUnm(long threadId) {
		Cursor c = mContext.getContentResolver().query(
				Uri.parse("content://sms"), null, "thread_id=? and read=?",
				new String[] { threadId + "", "0" }, null);
		return c;
	}
    public final void unbind() {
        if (Log.isLoggable(LogTag.CONTACT, Log.DEBUG)) {
            Log.v(TAG, "unbind: contacts.removeListeners " + this);
        }
        // Unregister contact update callbacks.
        Contact.removeListener(this);
    }
    
    public void setCheckBoxVisibility(int visibility){
    	mAvatarCheckBox.setVisibility(visibility);
    }

    public void setChecked(boolean checked) {
        mConversation.setIsChecked(checked);
        mAvatarCheckBox.setChecked(checked);
        updateBackground();
    }

    public boolean isChecked() {
        return mConversation.isChecked();
    }

    public void toggle() {
        mConversation.setIsChecked(!mConversation.isChecked());
        mAvatarCheckBox.setChecked(!mConversation.isChecked());
    }
}
